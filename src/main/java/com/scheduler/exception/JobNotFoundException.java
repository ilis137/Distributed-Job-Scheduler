package com.scheduler.exception;

/**
 * Exception thrown when a requested job cannot be found.
 */
public class JobNotFoundException extends SchedulerException {

    private final Long jobId;

    public JobNotFoundException(Long jobId) {
        super(String.format("Job not found with ID: %d", jobId));
        this.jobId = jobId;
    }

    public JobNotFoundException(String jobName) {
        super(String.format("Job not found with name: %s", jobName));
        this.jobId = null;
    }

    public Long getJobId() {
        return jobId;
    }
}

