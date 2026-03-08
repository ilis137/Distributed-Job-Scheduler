package com.scheduler.service;

import com.scheduler.coordination.FencingTokenProvider;
import com.scheduler.domain.entity.Job;
import com.scheduler.domain.enums.JobStatus;
import com.scheduler.exception.InvalidJobStateException;
import com.scheduler.exception.JobNotFoundException;
import com.scheduler.exception.StaleExecutionException;
import com.scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing job lifecycle and CRUD operations.
 *
 * Handles job creation, updates, deletion, and state transitions.
 * Enforces business rules and state machine validation.
 *
 * Interview Talking Points:
 * - "I use @Transactional to ensure atomic operations and consistency"
 * - "State transitions are validated using the JobStatus state machine"
 * - "Optimistic locking prevents concurrent modification conflicts"
 * - "Fencing tokens prevent zombie executions from updating job status"
 * - "I separate read and write operations for clarity and performance"
 *
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final FencingTokenProvider fencingTokenProvider;
    
    /**
     * Creates a new job.
     * 
     * @param job the job to create
     * @return the created job with generated ID
     */
    @Transactional
    public Job createJob(Job job) {
        log.info("Creating new job: {}", job.getName());
        
        // Validate job
        validateJob(job);
        
        // Set initial state
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.PENDING);
        }
        
        // Save job
        Job savedJob = jobRepository.save(job);
        
        log.info("Created job: {} with ID: {}", savedJob.getName(), savedJob.getId());
        
        return savedJob;
    }
    
    /**
     * Updates an existing job.
     * 
     * @param job the job to update
     * @return the updated job
     * @throws JobNotFoundException if job doesn't exist
     * @throws OptimisticLockingFailureException if concurrent modification detected
     */
    @Transactional
    public Job updateJob(Job job) {
        log.info("Updating job: {} (ID: {})", job.getName(), job.getId());
        
        // Verify job exists
        if (!jobRepository.existsById(job.getId())) {
            throw new JobNotFoundException(job.getId());
        }
        
        // Validate job
        validateJob(job);
        
        // Save job (optimistic locking will detect concurrent modifications)
        Job updatedJob = jobRepository.save(job);
        
        log.info("Updated job: {} (ID: {})", updatedJob.getName(), updatedJob.getId());
        
        return updatedJob;
    }
    
    /**
     * Deletes a job by ID.
     * 
     * @param jobId the job ID
     * @throws JobNotFoundException if job doesn't exist
     */
    @Transactional
    public void deleteJob(Long jobId) {
        log.info("Deleting job with ID: {}", jobId);
        
        Job job = findById(jobId);
        
        jobRepository.delete(job);
        
        log.info("Deleted job: {} (ID: {})", job.getName(), jobId);
    }
    
    /**
     * Finds a job by ID.
     * 
     * @param jobId the job ID
     * @return the job
     * @throws JobNotFoundException if job doesn't exist
     */
    @Transactional(readOnly = true)
    public Job findById(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
    }
    
    /**
     * Finds a job by name.
     * 
     * @param name the job name
     * @return the job
     * @throws JobNotFoundException if job doesn't exist
     */
    @Transactional(readOnly = true)
    public Job findByName(String name) {
        return jobRepository.findByName(name)
            .orElseThrow(() -> new JobNotFoundException(name));
    }
    
    /**
     * Finds all jobs.
     * 
     * @return list of all jobs
     */
    @Transactional(readOnly = true)
    public List<Job> findAll() {
        return jobRepository.findAll();
    }
    
    /**
     * Finds jobs by status.
     * 
     * @param status the job status
     * @return list of jobs with the given status
     */
    @Transactional(readOnly = true)
    public List<Job> findByStatus(JobStatus status) {
        return jobRepository.findByStatus(status);
    }
    
    /**
     * Finds jobs that are due for execution.
     * 
     * This is the critical query for job scheduling.
     * 
     * @param limit maximum number of jobs to return
     * @return list of due jobs
     */
    @Transactional(readOnly = true)
    public List<Job> findDueJobs(int limit) {
        return jobRepository.findDueJobs(Instant.now(), limit);
    }
    
    /**
     * Transitions a job to a new status with validation.
     *
     * Interview Talking Point:
     * "I validate state transitions using the state machine in JobStatus.
     * This prevents invalid transitions like COMPLETED → RUNNING that could
     * occur due to race conditions in distributed systems."
     *
     * @param jobId the job ID
     * @param targetStatus the target status
     * @return the updated job
     * @throws JobNotFoundException if job doesn't exist
     * @throws InvalidJobStateException if transition is invalid
     */
    @Transactional
    public Job transitionJobStatus(Long jobId, JobStatus targetStatus) {
        Job job = findById(jobId);

        log.info("Transitioning job {} from {} to {}", job.getName(), job.getStatus(), targetStatus);

        // Validate transition
        if (!job.canTransitionTo(targetStatus)) {
            throw new InvalidJobStateException(job.getStatus(), targetStatus);
        }

        // Perform transition
        job.transitionTo(targetStatus);

        // Save job
        Job updatedJob = jobRepository.save(job);

        log.info("Transitioned job {} to {}", updatedJob.getName(), updatedJob.getStatus());

        return updatedJob;
    }

    /**
     * Marks a job as scheduled.
     *
     * @param jobId the job ID
     * @return the updated job
     */
    @Transactional
    public Job scheduleJob(Long jobId) {
        return transitionJobStatus(jobId, JobStatus.SCHEDULED);
    }

    /**
     * Marks a job as running with fencing token validation.
     *
     * Interview Talking Point:
     * "I validate the fencing token before transitioning to RUNNING to ensure
     * only the current leader can start job execution. This prevents zombie
     * executions from starting after losing leadership."
     *
     * @param jobId the job ID
     * @param fencingToken the fencing token from the execution
     * @return the updated job
     * @throws StaleExecutionException if the fencing token is stale
     */
    @Transactional
    public Job startJob(Long jobId, String fencingToken) {
        validateFencingToken(jobId, fencingToken);

        Job job = transitionJobStatus(jobId, JobStatus.RUNNING);
        job.setLastExecutedAt(Instant.now());
        return jobRepository.save(job);
    }

    /**
     * Marks a job as completed with fencing token validation.
     *
     * Interview Talking Point:
     * "I validate the fencing token before marking a job as COMPLETED to prevent
     * zombie executions from updating the job status after losing their distributed
     * lock. Only the execution with the current epoch can complete the job."
     *
     * @param jobId the job ID
     * @param fencingToken the fencing token from the execution
     * @return the updated job
     * @throws StaleExecutionException if the fencing token is stale
     */
    @Transactional
    public Job completeJob(Long jobId, String fencingToken) {
        validateFencingToken(jobId, fencingToken);

        Job job = transitionJobStatus(jobId, JobStatus.COMPLETED);
        job.resetRetryCount();
        return jobRepository.save(job);
    }

    /**
     * Marks a job as failed with fencing token validation.
     *
     * Interview Talking Point:
     * "I validate the fencing token before marking a job as FAILED to prevent
     * duplicate failure handling. If two nodes execute the same job due to lock
     * expiration, only the current leader can mark it as failed and schedule retries."
     *
     * @param jobId the job ID
     * @param fencingToken the fencing token from the execution
     * @return the updated job
     * @throws StaleExecutionException if the fencing token is stale
     */
    @Transactional
    public Job failJob(Long jobId, String fencingToken) {
        validateFencingToken(jobId, fencingToken);

        Job job = transitionJobStatus(jobId, JobStatus.FAILED);
        job.incrementRetryCount();
        return jobRepository.save(job);
    }

    /**
     * Marks a job for retry with fencing token validation.
     *
     * @param jobId the job ID
     * @param nextRunTime the next retry time
     * @param fencingToken the fencing token from the execution
     * @return the updated job
     * @throws StaleExecutionException if the fencing token is stale
     */
    @Transactional
    public Job retryJob(Long jobId, Instant nextRunTime, String fencingToken) {
        validateFencingToken(jobId, fencingToken);

        Job job = transitionJobStatus(jobId, JobStatus.RETRYING);
        job.setNextRunTime(nextRunTime);
        return jobRepository.save(job);
    }

    /**
     * Moves a job to dead letter queue.
     *
     * @param jobId the job ID
     * @return the updated job
     */
    @Transactional
    public Job moveToDeadLetter(Long jobId) {
        return transitionJobStatus(jobId, JobStatus.DEAD_LETTER);
    }

    /**
     * Resets a job to pending status with fencing token validation.
     *
     * Used for recurring jobs after completion.
     *
     * @param jobId the job ID
     * @param nextRunTime the next run time
     * @param fencingToken the fencing token from the execution
     * @return the updated job
     * @throws StaleExecutionException if the fencing token is stale
     */
    @Transactional
    public Job resetToPending(Long jobId, Instant nextRunTime, String fencingToken) {
        validateFencingToken(jobId, fencingToken);

        Job job = transitionJobStatus(jobId, JobStatus.PENDING);
        job.setNextRunTime(nextRunTime);
        job.resetRetryCount();
        return jobRepository.save(job);
    }

    // ==================== Backward Compatibility Methods ====================

    /**
     * Marks a job as running (without fencing token validation).
     *
     * @deprecated Use {@link #startJob(Long, String)} with fencing token validation
     * @param jobId the job ID
     * @return the updated job
     */
    @Transactional
    @Deprecated
    public Job startJob(Long jobId) {
        Job job = transitionJobStatus(jobId, JobStatus.RUNNING);
        job.setLastExecutedAt(Instant.now());
        return jobRepository.save(job);
    }

    /**
     * Marks a job as completed (without fencing token validation).
     *
     * @deprecated Use {@link #completeJob(Long, String)} with fencing token validation
     * @param jobId the job ID
     * @return the updated job
     */
    @Transactional
    @Deprecated
    public Job completeJob(Long jobId) {
        Job job = transitionJobStatus(jobId, JobStatus.COMPLETED);
        job.resetRetryCount();
        return jobRepository.save(job);
    }

    /**
     * Marks a job as failed (without fencing token validation).
     *
     * @deprecated Use {@link #failJob(Long, String)} with fencing token validation
     * @param jobId the job ID
     * @return the updated job
     */
    @Transactional
    @Deprecated
    public Job failJob(Long jobId) {
        Job job = transitionJobStatus(jobId, JobStatus.FAILED);
        job.incrementRetryCount();
        return jobRepository.save(job);
    }

    /**
     * Marks a job for retry (without fencing token validation).
     *
     * @deprecated Use {@link #retryJob(Long, Instant, String)} with fencing token validation
     * @param jobId the job ID
     * @param nextRunTime the next retry time
     * @return the updated job
     */
    @Transactional
    @Deprecated
    public Job retryJob(Long jobId, Instant nextRunTime) {
        Job job = transitionJobStatus(jobId, JobStatus.RETRYING);
        job.setNextRunTime(nextRunTime);
        return jobRepository.save(job);
    }

    /**
     * Resets a job to pending status (without fencing token validation).
     *
     * @deprecated Use {@link #resetToPending(Long, Instant, String)} with fencing token validation
     * @param jobId the job ID
     * @param nextRunTime the next run time
     * @return the updated job
     */
    @Transactional
    @Deprecated
    public Job resetToPending(Long jobId, Instant nextRunTime) {
        Job job = transitionJobStatus(jobId, JobStatus.PENDING);
        job.setNextRunTime(nextRunTime);
        job.resetRetryCount();
        return jobRepository.save(job);
    }

    /**
     * Validates a job before saving.
     *
     * @param job the job to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateJob(Job job) {
        if (job.getName() == null || job.getName().isBlank()) {
            throw new IllegalArgumentException("Job name is required");
        }

        if (job.getMaxRetries() < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }

        if (job.getTimeoutSeconds() < 1) {
            throw new IllegalArgumentException("Timeout must be at least 1 second");
        }
    }

    /**
     * Validates a fencing token before allowing job status updates.
     *
     * Interview Talking Point:
     * "I validate fencing tokens on every critical job status update to prevent
     * zombie executions from corrupting state. This is a key distributed systems
     * pattern - even if a node loses its distributed lock, it can't update the
     * job status because its fencing token is stale."
     *
     * How it works:
     * 1. Extract epoch from the provided fencing token
     * 2. Compare with the current leader's epoch
     * 3. Reject if token is stale (from previous epoch)
     * 4. Reject if token is invalid (malformed or no current leader)
     *
     * @param jobId the job ID being updated
     * @param fencingToken the fencing token from the execution
     * @throws StaleExecutionException if the token is stale or invalid
     */
    private void validateFencingToken(Long jobId, String fencingToken) {
        if (fencingToken == null || fencingToken.isEmpty()) {
            log.error("Fencing token validation failed for job {}: token is null or empty", jobId);
            throw new StaleExecutionException(
                jobId,
                fencingToken,
                null,
                "Fencing token is required for job status updates"
            );
        }

        // Check if token is stale (from previous epoch)
        if (fencingTokenProvider.isTokenStale(fencingToken)) {
            Optional<String> currentToken = fencingTokenProvider.getCurrentFencingTokenString();
            String currentTokenStr = currentToken.orElse("unknown");

            long staleEpoch = fencingTokenProvider.extractEpochFromToken(fencingToken);
            long currentEpoch = fencingTokenProvider.extractEpochFromToken(currentTokenStr);

            log.warn(
                "Stale fencing token detected for job {}. " +
                "Stale token: {} (epoch {}), Current token: {} (epoch {}). " +
                "This execution lost leadership or its distributed lock expired.",
                jobId, fencingToken, staleEpoch, currentTokenStr, currentEpoch
            );

            throw new StaleExecutionException(jobId, fencingToken, currentTokenStr);
        }

        // Check if token is valid (matches current epoch)
        if (!fencingTokenProvider.isTokenValid(fencingToken)) {
            Optional<String> currentToken = fencingTokenProvider.getCurrentFencingTokenString();
            String currentTokenStr = currentToken.orElse("unknown");

            log.warn(
                "Invalid fencing token for job {}. " +
                "Provided token: {}, Current token: {}. " +
                "Token validation failed - possible leadership change or no current leader.",
                jobId, fencingToken, currentTokenStr
            );

            throw new StaleExecutionException(
                jobId,
                fencingToken,
                currentTokenStr,
                "Fencing token validation failed - token does not match current leader's epoch"
            );
        }

        log.debug("Fencing token validated successfully for job {}: {}", jobId, fencingToken);
    }
}

