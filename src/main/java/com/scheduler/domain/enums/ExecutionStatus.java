package com.scheduler.domain.enums;

/**
 * Represents the outcome of a single job execution attempt.
 * 
 * Interview Talking Point:
 * "I track execution status separately from job status to maintain a complete
 * audit trail. This is critical for debugging distributed systems issues and
 * understanding failure patterns."
 */
public enum ExecutionStatus {
    
    /**
     * Execution started successfully.
     * Initial state when execution record is created.
     */
    STARTED,
    
    /**
     * Execution completed successfully.
     * Job logic executed without errors.
     */
    SUCCESS,
    
    /**
     * Execution failed due to an error in job logic.
     * Will be retried if retry attempts remain.
     */
    FAILED,
    
    /**
     * Execution exceeded the configured timeout.
     * Distributed lock may have expired during execution.
     * 
     * Interview Talking Point:
     * "Timeouts are critical in distributed systems to prevent hung jobs from
     * blocking resources indefinitely. I set lock TTL to 2x job timeout as a safety margin."
     */
    TIMEOUT,
    
    /**
     * Execution was cancelled by user or system.
     * May occur during graceful shutdown.
     */
    CANCELLED,
    
    /**
     * Execution was skipped because another node is already executing this job.
     * Distributed lock was already held by another node.
     * 
     * Interview Talking Point:
     * "This status indicates the distributed locking is working correctly.
     * Multiple nodes may attempt to execute the same job, but only one succeeds
     * in acquiring the lock."
     */
    SKIPPED;
    
    /**
     * Checks if this status represents a successful execution.
     * 
     * @return true if execution was successful
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * Checks if this status represents a failed execution that should be retried.
     * 
     * @return true if execution failed and should be retried
     */
    public boolean shouldRetry() {
        return this == FAILED || this == TIMEOUT;
    }
    
    /**
     * Checks if this status represents a terminal state (no further action needed).
     * 
     * @return true if execution is in a terminal state
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == CANCELLED || this == SKIPPED;
    }
}

