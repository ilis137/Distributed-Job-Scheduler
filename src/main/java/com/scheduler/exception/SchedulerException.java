package com.scheduler.exception;

/**
 * Base exception for all scheduler-related errors.
 * Provides a foundation for the exception hierarchy.
 */
public class SchedulerException extends RuntimeException {

    public SchedulerException(String message) {
        super(message);
    }

    public SchedulerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchedulerException(Throwable cause) {
        super(cause);
    }
}

