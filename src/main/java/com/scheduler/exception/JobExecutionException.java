package com.scheduler.exception;

/**
 * Exception thrown when job execution fails.
 * Includes details about the failure for retry logic and error tracking.
 */
public class JobExecutionException extends SchedulerException {

    private final Long jobId;
    private final Long executionId;
    private final boolean retryable;

    public JobExecutionException(Long jobId, String message) {
        this(jobId, null, message, null, true);
    }

    public JobExecutionException(Long jobId, String message, Throwable cause) {
        this(jobId, null, message, cause, true);
    }

    public JobExecutionException(Long jobId, Long executionId, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.jobId = jobId;
        this.executionId = executionId;
        this.retryable = retryable;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

