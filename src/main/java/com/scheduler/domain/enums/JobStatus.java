package com.scheduler.domain.enums;

/**
 * Represents the lifecycle states of a job in the distributed scheduler.
 *
 * State Transitions:
 * PENDING → SCHEDULED → RUNNING → COMPLETED
 *        ↖           ↓         ↓
 *         ╰────────────────────╯ (recurring jobs)
 *                  FAILED ← RETRYING
 *                    ↓
 *              DEAD_LETTER
 *
 * Interview Talking Point:
 * "I implemented a state machine with validation to ensure jobs can only
 * transition through valid states, preventing corruption from race conditions.
 * For recurring jobs, I allow RUNNING → PENDING transition to reschedule the
 * job after successful execution, protected by fencing token validation."
 */
public enum JobStatus {
    
    /**
     * Job is created but not yet scheduled for execution.
     * Initial state when job is first created.
     */
    PENDING,
    
    /**
     * Job is due for execution and has been selected by the leader.
     * Leader is attempting to acquire distributed lock.
     */
    SCHEDULED,
    
    /**
     * Job is currently being executed by a worker thread.
     * Distributed lock is held, fencing token is active.
     */
    RUNNING,
    
    /**
     * Job completed successfully.
     * Terminal state (unless job is recurring).
     */
    COMPLETED,
    
    /**
     * Job execution failed.
     * Will transition to RETRYING if retries remain, or DEAD_LETTER if max retries exceeded.
     */
    FAILED,
    
    /**
     * Job is waiting to be retried after a failure.
     * Retry scheduled with exponential backoff.
     */
    RETRYING,
    
    /**
     * Job has exceeded max retry attempts and requires manual intervention.
     * Terminal state.
     */
    DEAD_LETTER,
    
    /**
     * Job has been paused by user or system.
     * Can be resumed to PENDING state.
     */
    PAUSED;
    
    /**
     * Validates if a transition from this status to the target status is allowed.
     *
     * Interview Talking Point:
     * "This prevents invalid state transitions that could occur due to race conditions
     * in a distributed system, such as marking a COMPLETED job as RUNNING. For recurring
     * jobs, I allow RUNNING → PENDING to reschedule after successful execution, but this
     * is protected by fencing token validation to prevent zombie executions from corrupting
     * the job state."
     *
     * @param targetStatus the status to transition to
     * @return true if transition is valid, false otherwise
     */
    public boolean canTransitionTo(JobStatus targetStatus) {
        if (this == targetStatus) {
            return true; // Same state is always valid
        }

        return switch (this) {
            case PENDING -> targetStatus == SCHEDULED || targetStatus == PAUSED;
            case SCHEDULED -> targetStatus == RUNNING || targetStatus == FAILED || targetStatus == PENDING;
            case RUNNING -> targetStatus == COMPLETED || targetStatus == FAILED || targetStatus == PENDING;
            case FAILED -> targetStatus == RETRYING || targetStatus == DEAD_LETTER;
            case RETRYING -> targetStatus == SCHEDULED || targetStatus == DEAD_LETTER;
            case COMPLETED -> targetStatus == PENDING; // For recurring jobs
            case PAUSED -> targetStatus == PENDING;
            case DEAD_LETTER -> false; // Terminal state
        };
    }
    
    /**
     * Checks if this status represents a terminal state.
     * Terminal states cannot transition to other states (except for special cases).
     * 
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return this == DEAD_LETTER;
    }
    
    /**
     * Checks if this status represents an active execution state.
     * 
     * @return true if job is actively being executed
     */
    public boolean isActive() {
        return this == RUNNING;
    }
    
    /**
     * Checks if this status represents a failed state.
     * 
     * @return true if job has failed or is in dead letter queue
     */
    public boolean isFailedState() {
        return this == FAILED || this == DEAD_LETTER;
    }
}

