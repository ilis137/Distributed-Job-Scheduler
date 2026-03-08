package com.scheduler.exception;

import com.scheduler.domain.enums.JobStatus;

/**
 * Exception thrown when attempting an invalid job state transition.
 */
public class InvalidJobStateException extends SchedulerException {

    private final JobStatus currentStatus;
    private final JobStatus targetStatus;

    public InvalidJobStateException(JobStatus currentStatus, JobStatus targetStatus) {
        super(String.format("Invalid state transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public JobStatus getCurrentStatus() {
        return currentStatus;
    }

    public JobStatus getTargetStatus() {
        return targetStatus;
    }
}

