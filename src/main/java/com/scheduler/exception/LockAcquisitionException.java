package com.scheduler.exception;

/**
 * Exception thrown when distributed lock acquisition fails.
 */
public class LockAcquisitionException extends SchedulerException {

    private final String lockKey;

    public LockAcquisitionException(String lockKey) {
        super(String.format("Failed to acquire lock: %s", lockKey));
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, Throwable cause) {
        super(String.format("Failed to acquire lock: %s", lockKey), cause);
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}

