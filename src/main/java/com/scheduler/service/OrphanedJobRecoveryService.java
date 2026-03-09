package com.scheduler.service;

import com.scheduler.config.SchedulerProperties;
import com.scheduler.coordination.LeaderElectionService;
import com.scheduler.domain.entity.Job;
import com.scheduler.executor.RetryManager;
import com.scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for recovering orphaned jobs that are stuck in RUNNING status.
 *
 * Orphaned jobs occur when a node crashes or loses leadership while executing a job,
 * leaving the job stuck in RUNNING status without being completed or failed.
 *
 * This service periodically scans for such jobs and marks them as FAILED so they
 * can be retried according to the retry policy.
 *
 * Interview Talking Points:
 * - "I implemented active orphaned job recovery because passive lock expiration alone
 *   doesn't update the job status in the database. This scheduled task runs every 60
 *   seconds on the leader to find jobs stuck in RUNNING status for longer than their
 *   timeout and marks them as FAILED so they can be retried. This ensures jobs aren't
 *   lost when nodes crash."
 * - "The recovery task only runs on the leader to prevent duplicate recovery attempts"
 * - "I use a conservative threshold (5 minutes) to avoid false positives for long-running jobs"
 * - "Each recovered job goes through the normal retry flow with exponential backoff"
 *
 * Design Decision:
 * "I chose to implement active recovery (periodic scanning) in addition to passive
 * recovery (lock expiration) because:
 * 1. Lock expiration only releases the lock in Redis - it doesn't update job status
 * 2. Without active recovery, orphaned jobs would remain in RUNNING status forever
 * 3. Active recovery provides a safety net for edge cases like network partitions
 * 4. The overhead is minimal - one query every 60 seconds on the leader only"
 *
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "scheduler.recovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrphanedJobRecoveryService {

    private final JobRepository jobRepository;
    private final JobService jobService;
    private final RetryManager retryManager;
    private final LeaderElectionService leaderElectionService;
    private final SchedulerProperties properties;

    private final AtomicInteger recoveryCount = new AtomicInteger(0);
    private final AtomicInteger totalJobsRecovered = new AtomicInteger(0);

    /**
     * Recovers orphaned jobs that are stuck in RUNNING status.
     *
     * This task runs periodically (default: every 60 seconds) and only on the leader node.
     * It finds jobs that have been in RUNNING status for longer than the configured threshold
     * and marks them as FAILED so they can be retried.
     *
     * Interview Talking Point:
     * "The recovery task uses a conservative threshold (5 minutes by default) to avoid
     * false positives. This is significantly longer than typical job timeouts, so we only
     * recover jobs that are truly stuck. The task only runs on the leader to prevent
     * duplicate recovery attempts across the cluster."
     */
    @Scheduled(
        fixedDelayString = "${scheduler.recovery.interval-seconds:60}000",
        initialDelayString = "${scheduler.recovery.initial-delay-seconds:30}000"
    )
    public void recoverOrphanedJobs() {
        // Only leader should perform recovery
        if (!leaderElectionService.isLeader()) {
            return;
        }

        int currentRecovery = recoveryCount.incrementAndGet();

        try {
            // Log every 10 recovery cycles (every 10 minutes by default)
            if (currentRecovery % 10 == 0) {
                log.debug("Orphaned job recovery task running - cycle #{}, total jobs recovered: {}",
                    currentRecovery, totalJobsRecovered.get());
            }

            // Calculate threshold for stuck jobs
            int thresholdMinutes = properties.getRecovery().getStuckJobThresholdMinutes();
            Instant threshold = Instant.now().minus(Duration.ofMinutes(thresholdMinutes));

            // Find stuck jobs
            List<Job> stuckJobs = jobRepository.findStuckJobs(threshold);

            if (!stuckJobs.isEmpty()) {
                log.warn("Found {} orphaned jobs stuck in RUNNING status - initiating recovery",
                    stuckJobs.size());

                // Recover each stuck job
                for (Job job : stuckJobs) {
                    try {
                        recoverOrphanedJob(job);
                        totalJobsRecovered.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to recover orphaned job: {} (ID: {})",
                            job.getName(), job.getId(), e);
                        // Continue with next job - don't let one failure stop recovery
                    }
                }

                log.info("Orphaned job recovery completed - recovered {} jobs", stuckJobs.size());
            }

        } catch (Exception e) {
            log.error("Error during orphaned job recovery task", e);
        }
    }

    /**
     * Recovers a single orphaned job.
     *
     * Marks the job as FAILED and either schedules a retry or moves it to the
     * dead letter queue based on the retry policy.
     *
     * Interview Talking Point:
     * "When recovering an orphaned job, I mark it as FAILED using the deprecated
     * method (without fencing token validation) because this is a recovery operation,
     * not a normal execution flow. The job is truly abandoned, so we need to bypass
     * the fencing token check. Then I follow the normal retry logic - if retries
     * remain, schedule a retry with exponential backoff; otherwise, move to dead
     * letter queue."
     *
     * @param job the orphaned job to recover
     */
    @Transactional
    protected void recoverOrphanedJob(Job job) {
        log.warn("Recovering orphaned job: {} (ID: {}, last updated: {}, stuck for: {} minutes)",
            job.getName(),
            job.getId(),
            job.getUpdatedAt(),
            Duration.between(job.getUpdatedAt(), Instant.now()).toMinutes());

        try {
            // Mark job as FAILED (use deprecated method without fencing token - this is recovery)
            // We bypass fencing token validation because the original executor is gone
            jobService.failJob(job.getId());

            // Reload job to get updated retry count
            Job updatedJob = jobService.findById(job.getId());

            // Check if retry is needed
            if (retryManager.shouldRetry(updatedJob)) {
                log.info("Orphaned job {} will be retried (attempt {}/{})",
                    updatedJob.getName(),
                    updatedJob.getRetryCount(),
                    updatedJob.getMaxRetries());

                // Calculate next retry time with exponential backoff
                Instant nextRetryTime = retryManager.calculateNextRetryTime(updatedJob);

                // Schedule retry (use deprecated method without fencing token)
                jobService.retryJob(updatedJob.getId(), nextRetryTime);

                log.info("Orphaned job {} scheduled for retry at {}",
                    updatedJob.getName(), nextRetryTime);
            } else {
                log.warn("Orphaned job {} has exhausted all retries - moving to dead letter queue",
                    updatedJob.getName());

                // Move to dead letter queue
                jobService.moveToDeadLetter(updatedJob.getId());

                log.warn("Orphaned job {} moved to dead letter queue", updatedJob.getName());
            }

        } catch (Exception e) {
            log.error("Error recovering orphaned job: {} (ID: {})",
                job.getName(), job.getId(), e);
            throw e; // Re-throw to be caught by caller
        }
    }

    /**
     * Gets the total number of recovery cycles performed.
     *
     * @return the recovery count
     */
    public int getRecoveryCount() {
        return recoveryCount.get();
    }

    /**
     * Gets the total number of jobs recovered.
     *
     * @return the total jobs recovered
     */
    public int getTotalJobsRecovered() {
        return totalJobsRecovered.get();
    }

    /**
     * Resets the counters.
     *
     * Used for testing.
     */
    public void resetCounters() {
        recoveryCount.set(0);
        totalJobsRecovered.set(0);
    }
}

