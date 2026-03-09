package com.scheduler.exception;

/**
 * Exception thrown when a job execution is not found.
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public class JobExecutionNotFoundException extends RuntimeException {
    
    private final Long executionId;
    
    public JobExecutionNotFoundException(Long executionId) {
        super("Job execution not found with ID: " + executionId);
        this.executionId = executionId;
    }
    
    public Long getExecutionId() {
        return executionId;
    }
}

