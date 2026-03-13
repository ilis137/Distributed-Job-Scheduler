package com.scheduler.service;

import com.scheduler.config.SchedulerProperties;
import com.scheduler.coordination.FencingTokenProvider;
import com.scheduler.domain.entity.Job;
import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.domain.enums.ExecutionStatus;
import com.scheduler.repository.JobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing job execution records.
 * 
 * Tracks execution history with fencing tokens for split-brain prevention.
 * Records execution duration, errors, and retry attempts.
 * 
 * Interview Talking Points:
 * - "I create execution records with fencing tokens to prevent zombie leaders
 *   from corrupting state after network partitions"
 * - "Each execution record tracks duration, errors, and retry attempts for
 *   debugging and performance monitoring"
 * - "Fencing tokens contain the epoch number from leader election"
 * - "The database can validate that writes come from the current leader"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {
    
    private final JobExecutionRepository executionRepository;
    private final FencingTokenProvider fencingTokenProvider;
    private final SchedulerProperties properties;
    
    /**
     * Creates a new execution record for a job.
     * 
     * Interview Talking Point:
     * "I create an execution record at the start of job execution with a
     * fencing token. This provides an audit trail and prevents split-brain
     * scenarios where two leaders try to execute the same job."
     * 
     * @param job the job being executed
     * @param node the scheduler node executing the job
     * @param retryAttempt the retry attempt number (0 for first attempt)
     * @return the created execution record
     */
    @Transactional
    public JobExecution createExecution(Job job, SchedulerNode node, int retryAttempt) {
        log.info("Creating execution record for job: {} (retry: {})", job.getName(), retryAttempt);
        
        // Get fencing token
        String fencingToken = node.generateFencingToken();
        
        // Create execution record
        JobExecution execution = JobExecution.builder()
            .job(job)
            .fencingToken(fencingToken)
            .nodeId(node.getNodeId())
            .status(ExecutionStatus.STARTED)
            .startedAt(Instant.now())
            .retryAttempt(retryAttempt)
            .build();
        
        // Save execution
        JobExecution savedExecution = executionRepository.save(execution);
        
        log.info("Created execution record: {} for job: {} with fencing token: {}",
            savedExecution.getId(), job.getName(), fencingToken);
        
        return savedExecution;
    }
    
    /**
     * Marks an execution as successful.
     * 
     * @param executionId the execution ID
     * @param result the execution result (optional)
     * @return the updated execution
     */
    @Transactional
    public JobExecution markSuccess(Long executionId, String result) {
        JobExecution execution = findById(executionId);
        
        log.info("Marking execution {} as SUCCESS", executionId);
        
        execution.succeed(result);
        
        return executionRepository.save(execution);
    }
    
    /**
     * Marks an execution as failed.
     * 
     * @param executionId the execution ID
     * @param errorMessage the error message
     * @param errorStackTrace the error stack trace
     * @return the updated execution
     */
    @Transactional
    public JobExecution markFailed(Long executionId, String errorMessage, String errorStackTrace) {
        JobExecution execution = findById(executionId);
        
        log.warn("Marking execution {} as FAILED: {}", executionId, errorMessage);
        
        execution.fail(errorMessage, errorStackTrace);
        
        return executionRepository.save(execution);
    }
    
    /**
     * Marks an execution as timed out.
     * 
     * @param executionId the execution ID
     * @return the updated execution
     */
    @Transactional
    public JobExecution markTimeout(Long executionId) {
        JobExecution execution = findById(executionId);
        
        log.warn("Marking execution {} as TIMEOUT", executionId);
        
        execution.timeout();
        
        return executionRepository.save(execution);
    }
    
    /**
     * Marks an execution as cancelled.
     * 
     * @param executionId the execution ID
     * @return the updated execution
     */
    @Transactional
    public JobExecution markCancelled(Long executionId) {
        JobExecution execution = findById(executionId);
        
        log.info("Marking execution {} as CANCELLED", executionId);
        
        execution.complete(ExecutionStatus.CANCELLED);
        
        return executionRepository.save(execution);
    }
    
    /**
     * Marks an execution as skipped.
     *
     * Used when distributed lock cannot be acquired.
     *
     * @param executionId the execution ID
     * @return the updated execution
     */
    @Transactional
    public JobExecution markSkipped(Long executionId) {
        JobExecution execution = findById(executionId);

        log.debug("Marking execution {} as SKIPPED (lock held by another node)", executionId);

        execution.complete(ExecutionStatus.SKIPPED);

        return executionRepository.save(execution);
    }

    /**
     * Finds an execution by ID.
     *
     * @param executionId the execution ID
     * @return the execution
     * @throws IllegalArgumentException if execution doesn't exist
     */
    @Transactional(readOnly = true)
    public JobExecution findById(Long executionId) {
        return executionRepository.findById(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
    }

    /**
     * Finds an execution by ID with the Job association eagerly fetched.
     *
     * This method is used by API endpoints that need to return job details
     * in the execution response. It uses @EntityGraph in the repository to
     * fetch the Job association in a single query, preventing LazyInitializationException.
     *
     * Interview Talking Point:
     * "I have a separate method for fetching executions with job details to avoid
     * the LazyInitializationException. This eagerly fetches the Job association using
     * @EntityGraph, which generates a single JOIN query instead of N+1 queries. The
     * default findById() still uses lazy loading for internal use cases where job
     * details aren't needed."
     *
     * @param executionId the execution ID
     * @return the execution with job association loaded
     * @throws IllegalArgumentException if execution doesn't exist
     */
    @Transactional(readOnly = true)
    public JobExecution findByIdWithJob(Long executionId) {
        return executionRepository.findWithJobById(executionId)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
    }

    /**
     * Finds all executions.
     *
     * @param pageable pagination parameters
     * @return page of executions
     */
    @Transactional(readOnly = true)
    public Page<JobExecution> findAll(Pageable pageable) {
        return executionRepository.findAll(pageable);
    }

    /**
     * Finds all executions for a job.
     *
     * @param jobId the job ID
     * @return list of executions
     */
    @Transactional(readOnly = true)
    public List<JobExecution> findByJobId(Long jobId) {
        return executionRepository.findByJobId(jobId);
    }

    /**
     * Finds all executions for a job (paginated).
     *
     * @param jobId the job ID
     * @param pageable pagination parameters
     * @return page of executions
     */
    @Transactional(readOnly = true)
    public Page<JobExecution> findByJobId(Long jobId, Pageable pageable) {
        return executionRepository.findByJobId(jobId, pageable);
    }

    /**
     * Finds the latest execution for a job.
     *
     * @param jobId the job ID
     * @return the latest execution, or null if none exists
     */
    @Transactional(readOnly = true)
    public JobExecution findLatestByJobId(Long jobId) {
        return executionRepository.findLatestByJobId(jobId).orElse(null);
    }

    /**
     * Finds executions by status.
     *
     * @param status the execution status
     * @return list of executions
     */
    @Transactional(readOnly = true)
    public List<JobExecution> findByStatus(ExecutionStatus status) {
        return executionRepository.findByStatus(status);
    }

    /**
     * Finds executions by status (paginated).
     *
     * @param status the execution status
     * @param pageable pagination parameters
     * @return page of executions
     */
    @Transactional(readOnly = true)
    public Page<JobExecution> findByStatus(ExecutionStatus status, Pageable pageable) {
        return executionRepository.findByStatus(status, pageable);
    }

    /**
     * Validates a fencing token against the current leader's token.
     *
     * Interview Talking Point:
     * "I validate fencing tokens to ensure writes come from the current leader.
     * If a zombie leader tries to write with an old epoch, the validation fails
     * and the write is rejected."
     *
     * @param fencingToken the fencing token to validate
     * @return true if the token is valid
     */
    public boolean validateFencingToken(String fencingToken) {
        if (fencingToken == null || !fencingToken.startsWith("epoch")) {
            log.warn("Invalid fencing token format: {}", fencingToken);
            return false;
        }

        try {
            // Extract epoch from token
            String epochPart = fencingToken.substring(5, fencingToken.indexOf("-node"));
            long tokenEpoch = Long.parseLong(epochPart);

            // Get current epoch
            Long currentEpoch = fencingTokenProvider.getCurrentFencingToken().orElse(null);

            if (currentEpoch == null) {
                log.warn("No current leader - cannot validate fencing token");
                return false;
            }

            // Validate epoch
            boolean valid = tokenEpoch == currentEpoch;

            if (!valid) {
                log.warn("Stale fencing token detected: {} (current epoch: {})", tokenEpoch, currentEpoch);
            }

            return valid;
        } catch (Exception e) {
            log.error("Error validating fencing token: {}", fencingToken, e);
            return false;
        }
    }
}

