package com.scheduler.executor;

import com.scheduler.coordination.DistributedLockService;
import com.scheduler.coordination.LeaderElectionService;
import com.scheduler.domain.entity.Job;
import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.domain.enums.ExecutionStatus;
import com.scheduler.domain.enums.JobStatus;
import com.scheduler.exception.JobExecutionException;
import com.scheduler.exception.StaleExecutionException;
import com.scheduler.service.JobExecutionService;
import com.scheduler.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Executes jobs in virtual threads with distributed locking.
 *
 * Handles the complete job execution lifecycle:
 * 1. Acquire distributed lock
 * 2. Create execution record
 * 3. Execute job in virtual thread
 * 4. Handle success/failure/timeout
 * 5. Release lock
 * 6. Update job status
 * 7. Schedule retry if needed
 *
 * Interview Talking Points:
 * - "I use virtual threads for high concurrency - can handle 10,000+ concurrent jobs"
 * - "Distributed locks prevent duplicate execution across nodes"
 * - "Fencing tokens prevent zombie leaders from corrupting state"
 * - "Timeout handling prevents hung jobs from blocking resources"
 * - "Retry logic with exponential backoff handles transient failures"
 *
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobExecutor {

    private final JobService jobService;
    private final JobExecutionService executionService;
    private final DistributedLockService lockService;
    private final LeaderElectionService leaderElectionService;
    private final RetryManager retryManager;

    @Qualifier("jobExecutorService")
    private final ExecutorService executorService;

    /**
     * Executes a job asynchronously in a virtual thread.
     *
     * Interview Talking Point:
     * "I execute jobs asynchronously using CompletableFuture and virtual threads.
     * This allows the scheduler to submit thousands of jobs without blocking.
     * Virtual threads are cheap to create and destroy, so I create one per job."
     *
     * @param job the job to execute
     * @return CompletableFuture that completes when job finishes
     */
    public CompletableFuture<Void> executeAsync(Job job) {
        return CompletableFuture.runAsync(() -> execute(job), executorService);
    }

    /**
     * Executes a job synchronously.
     *
     * This is the main execution method that handles the complete lifecycle.
     *
     * @param job the job to execute
     */
    public void execute(Job job) {
        String lockKey = lockService.createJobLockKey(job.getId());

        log.info("Attempting to execute job: {} (ID: {})", job.getName(), job.getId());

        // Try to acquire distributed lock
        boolean lockAcquired = lockService.tryAcquire(
            lockKey,
            Duration.ofSeconds(job.getTimeoutSeconds() * 2) // Lock TTL = 2x job timeout
        );

        if (!lockAcquired) {
            log.debug("Failed to acquire lock for job: {} - another node is executing it", job.getName());
            return;
        }

        try {
            // Execute job with lock held
            executeWithLock(job);
        } finally {
            // Always release lock
            lockService.release(lockKey);
            log.debug("Released lock for job: {}", job.getName());
        }
    }

    /**
     * Executes a job while holding the distributed lock.
     *
     * Interview Talking Point:
     * "I validate the fencing token at every critical point in the job lifecycle
     * to prevent zombie executions from updating job status. Even if a node loses
     * its distributed lock mid-execution, it can't corrupt the job state because
     * its fencing token is stale."
     *
     * @param job the job to execute
     */
    private void executeWithLock(Job job) {
        JobExecution execution = null;
        String fencingToken = null;

        try {
            // Get current node
            SchedulerNode currentNode = leaderElectionService.getCurrentNode();

            // Create execution record with fencing token
            execution = executionService.createExecution(job, currentNode, job.getRetryCount());
            fencingToken = execution.getFencingToken();

            log.info("Executing job {} with fencing token: {}", job.getName(), fencingToken);

            // Transition job to RUNNING (with fencing token validation)
            try {
                jobService.startJob(job.getId(), fencingToken);
            } catch (StaleExecutionException e) {
                log.warn("Stale execution detected when starting job {}: {}", job.getName(), e.getMessage());
                // Mark execution as cancelled - we lost leadership
                executionService.markCancelled(execution.getId());
                return; // Abort execution
            }

            // Execute job with timeout
            executeJobWithTimeout(job, execution);

            // Mark execution as successful
            executionService.markSuccess(execution.getId(), "Job completed successfully");

            // Handle post-execution (recurring jobs, completion) with fencing token validation
            handleSuccessfulExecution(job, fencingToken);

            log.info("Job {} completed successfully", job.getName());

        } catch (Exception e) {
            log.error("Job {} failed with error", job.getName(), e);

            // Mark execution as failed
            if (execution != null) {
                String errorMessage = e.getMessage();
                String stackTrace = getStackTrace(e);
                executionService.markFailed(execution.getId(), errorMessage, stackTrace);
            }

            // Handle failure (retry or dead letter) with fencing token validation
            handleFailedExecution(job, fencingToken, e);
        }
    }

    /**
     * Executes the actual job logic with timeout.
     *
     * Interview Talking Point:
     * "I use CompletableFuture.orTimeout() to enforce job timeouts.
     * If a job exceeds its timeout, the future completes exceptionally
     * and I mark the execution as TIMEOUT. This prevents hung jobs from
     * blocking resources indefinitely."
     *
     * @param job the job to execute
     * @param execution the execution record
     * @throws Exception if job execution fails or times out
     */
    private void executeJobWithTimeout(Job job, JobExecution execution) throws Exception {
        log.info("Executing job: {} with timeout: {}s", job.getName(), job.getTimeoutSeconds());

        // Create a future for job execution
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // Execute the actual job logic
                executeJobLogic(job);
            } catch (Exception e) {
                throw new JobExecutionException(job.getId(), "Job execution failed", e);
            }
        }, executorService);

        // Apply timeout
        future.orTimeout(job.getTimeoutSeconds(), TimeUnit.SECONDS);

        try {
            // Wait for completion
            future.join();
        } catch (Exception e) {
            // Check if it was a timeout
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.warn("Job {} timed out after {}s", job.getName(), job.getTimeoutSeconds());
                executionService.markTimeout(execution.getId());
                throw new JobExecutionException(job.getId(), "Job timed out", e.getCause());
            }
            throw e;
        }
    }

    /**
     * Executes the actual job logic.
     *
     * This is a placeholder for the actual job implementation.
     * In a real system, this would:
     * - Deserialize the job payload
     * - Invoke the appropriate job handler
     * - Execute the business logic
     *
     * Interview Talking Point:
     * "In a production system, I would have a JobHandler interface with
     * different implementations for different job types. The payload would
     * contain the job type and parameters, and I would use a factory pattern
     * to instantiate the correct handler."
     *
     * @param job the job to execute
     * @throws Exception if job execution fails
     */
    private void executeJobLogic(Job job) throws Exception {
        log.info("Executing job logic for: {}", job.getName());

        // Placeholder: In a real system, this would execute the actual job logic
        // For now, we'll simulate work with a sleep

        // Parse payload (in real system, this would be JSON with job parameters)
        String payload = job.getPayload();

        if (payload != null && payload.contains("\"simulateFailure\":true")) {
            throw new RuntimeException("Simulated job failure");
        }

        if (payload != null && payload.contains("\"duration\":")) {
            // Extract duration from payload
            int duration = extractDuration(payload);
            Thread.sleep(duration);
        } else {
            // Default: simulate 1 second of work
            Thread.sleep(1000);
        }

        log.info("Job logic completed for: {}", job.getName());
    }

    /**
     * Handles successful job execution with fencing token validation.
     *
     * For recurring jobs: resets to PENDING with next run time calculated from cron expression
     * For one-time jobs: marks as COMPLETED
     *
     * Interview Talking Point:
     * "I validate the fencing token before updating job status after successful
     * execution. This prevents a zombie execution from marking a job as completed
     * after another node has already taken over and started re-executing it."
     *
     * Design Decision:
     * "For recurring jobs, I parse the cron expression to calculate the next run time.
     * This ensures jobs execute at their intended schedule (e.g., daily at midnight).
     * For one-time jobs, I mark them as COMPLETED since they don't need to run again."
     *
     * @param job the job that completed successfully
     * @param fencingToken the fencing token from the execution
     */
    private void handleSuccessfulExecution(Job job, String fencingToken) {
        try {
            if (job.getRecurring()) {
                log.info("Job {} is recurring - scheduling next execution", job.getName());

                // Calculate next run time from cron expression
                Instant nextRunTime = calculateNextRunTimeFromCron(job.getCronExpression());

                jobService.resetToPending(job.getId(), nextRunTime, fencingToken);

                log.info("Recurring job {} scheduled for next execution at {}",
                    job.getName(), nextRunTime);
            } else {
                log.info("Job {} is one-time - marking as completed", job.getName());
                jobService.completeJob(job.getId(), fencingToken);
            }
        } catch (StaleExecutionException e) {
            log.warn(
                "Stale execution detected when completing job {}: {}. " +
                "Another node may have taken over. Execution record preserved for audit.",
                job.getName(), e.getMessage()
            );
            // Don't rethrow - execution record is already marked as SUCCESS
            // This is just a zombie trying to update job status
        }
    }

    /**
     * Handles failed job execution with fencing token validation.
     *
     * If retries remain: schedules retry with exponential backoff
     * If no retries remain: moves to dead letter queue
     *
     * Interview Talking Point:
     * "I validate the fencing token before scheduling retries to prevent duplicate
     * retry scheduling. If two nodes execute the same job due to lock expiration,
     * only the current leader can mark it as failed and schedule retries."
     *
     * @param job the job that failed
     * @param fencingToken the fencing token from the execution
     * @param error the error that caused the failure
     */
    private void handleFailedExecution(Job job, String fencingToken, Exception error) {
        try {
            // Mark job as failed (increments retry count) with fencing token validation
            jobService.failJob(job.getId(), fencingToken);

            // Reload job to get updated retry count
            Job updatedJob = jobService.findById(job.getId());

            if (retryManager.shouldRetry(updatedJob)) {
                log.info("Job {} will be retried (attempt {}/{})",
                    updatedJob.getName(), updatedJob.getRetryCount(), updatedJob.getMaxRetries());

                // Calculate next retry time
                Instant nextRetryTime = retryManager.calculateNextRetryTime(updatedJob);

                // Schedule retry with fencing token validation
                try {
                    jobService.retryJob(updatedJob.getId(), nextRetryTime, fencingToken);
                } catch (StaleExecutionException e) {
                    log.warn(
                        "Stale execution detected when scheduling retry for job {}: {}. " +
                        "Another node may have taken over. Execution record preserved for audit.",
                        job.getName(), e.getMessage()
                    );
                }
            } else {
                log.warn("Job {} has exhausted all retries - moving to dead letter queue",
                    updatedJob.getName());

                // Move to dead letter queue (no fencing token needed - terminal state)
                jobService.moveToDeadLetter(updatedJob.getId());
            }
        } catch (StaleExecutionException e) {
            log.warn(
                "Stale execution detected when marking job {} as failed: {}. " +
                "Another node may have taken over. Execution record preserved for audit.",
                job.getName(), e.getMessage()
            );
            // Don't rethrow - execution record is already marked as FAILED
            // This is just a zombie trying to update job status
        }
    }

    /**
     * Extracts stack trace from an exception.
     *
     * @param e the exception
     * @return the stack trace as a string
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Extracts duration from job payload.
     *
     * @param payload the job payload
     * @return the duration in milliseconds
     */
    private int extractDuration(String payload) {
        try {
            // Simple extraction: "duration":1000
            int start = payload.indexOf("\"duration\":") + 11;
            int end = payload.indexOf(",", start);
            if (end == -1) {
                end = payload.indexOf("}", start);
            }
            return Integer.parseInt(payload.substring(start, end).trim());
        } catch (Exception e) {
            return 1000; // Default 1 second
        }
    }


    /**
     * Calculates next run time from cron expression.
     *
     * Uses Spring's CronExpression to parse and calculate the next occurrence.
     *
     * Interview Talking Point:
     * "I use Spring's CronExpression class for cron parsing because it's built into
     * Spring Framework 5.3+, avoiding external dependencies like Quartz. It supports
     * standard cron format and integrates seamlessly with Spring Boot."
     *
     * Design Decision:
     * "If the cron expression is invalid or has no future occurrences, I fall back to
     * scheduling 1 hour from now. This ensures the job doesn't get stuck and provides
     * a reasonable default for edge cases."
     *
     * @param cronExpression the cron expression
     * @return the next run time
     */
    private Instant calculateNextRunTimeFromCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            log.warn("No cron expression provided for recurring job - using 1 hour default");
            return Instant.now().plus(Duration.ofHours(1));
        }

        try {
            // Parse cron expression using Spring's CronExpression
            org.springframework.scheduling.support.CronExpression cron =
                org.springframework.scheduling.support.CronExpression.parse(cronExpression);

            // Calculate next occurrence from current time
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime next = cron.next(now);

            if (next == null) {
                log.warn("Cron expression '{}' has no future occurrences - using 1 hour default",
                    cronExpression);
                return Instant.now().plus(Duration.ofHours(1));
            }

            // Convert to Instant using system default timezone
            Instant nextRunTime = next.atZone(java.time.ZoneId.systemDefault()).toInstant();

            log.debug("Calculated next run time from cron expression '{}': {}",
                cronExpression, nextRunTime);

            return nextRunTime;

        } catch (IllegalArgumentException e) {
            log.error("Invalid cron expression '{}': {} - using 1 hour default",
                cronExpression, e.getMessage());
            return Instant.now().plus(Duration.ofHours(1));
        }
    }

}

