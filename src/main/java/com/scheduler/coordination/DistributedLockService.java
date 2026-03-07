package com.scheduler.coordination;

import com.scheduler.config.SchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Service for distributed locking using Redis Redlock algorithm.
 * 
 * Provides high-level distributed locking operations with automatic
 * lock management, renewal, and release.
 * 
 * Interview Talking Points:
 * - "I use the Redlock algorithm for distributed mutual exclusion"
 * - "Locks have TTL to prevent deadlocks if a node crashes while holding the lock"
 * - "For long-running jobs, I implement automatic lock renewal"
 * - "The lock key includes the job ID to ensure fine-grained locking"
 * - "I use try-with-resources pattern for automatic lock release"
 * 
 * Design Decision:
 * "I chose Redlock over database-based locking because:
 * 1. Lower latency - Redis is in-memory
 * 2. Automatic TTL expiry prevents deadlocks
 * 3. No database contention for lock acquisition
 * 4. Redisson provides battle-tested Redlock implementation"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
    
    private final CoordinationService coordinationService;
    private final SchedulerProperties properties;
    
    /**
     * Executes a task while holding a distributed lock.
     * 
     * Acquires the lock, executes the task, and releases the lock.
     * If lock acquisition fails, returns empty Optional.
     * 
     * Interview Talking Point:
     * "I use a functional approach with Supplier to ensure the lock is always
     * released, even if the task throws an exception. This prevents lock leaks."
     * 
     * @param lockKey the unique key for the lock
     * @param task the task to execute while holding the lock
     * @param <T> the return type of the task
     * @return Optional containing the task result, or empty if lock not acquired
     */
    public <T> Optional<T> executeWithLock(String lockKey, Supplier<T> task) {
        return executeWithLock(lockKey, getDefaultLockTTL(), task);
    }
    
    /**
     * Executes a task while holding a distributed lock with custom TTL.
     * 
     * @param lockKey the unique key for the lock
     * @param ttl the time-to-live for the lock
     * @param task the task to execute while holding the lock
     * @param <T> the return type of the task
     * @return Optional containing the task result, or empty if lock not acquired
     */
    public <T> Optional<T> executeWithLock(String lockKey, Duration ttl, Supplier<T> task) {
        boolean acquired = coordinationService.tryAcquireLock(lockKey, ttl);
        
        if (!acquired) {
            log.debug("Failed to acquire lock: {} - another node holds it", lockKey);
            return Optional.empty();
        }
        
        try {
            log.debug("Executing task with lock: {}", lockKey);
            T result = task.get();
            return Optional.of(result);
        } catch (Exception e) {
            log.error("Error executing task with lock: {}", lockKey, e);
            throw e;
        } finally {
            coordinationService.releaseLock(lockKey);
            log.debug("Released lock: {}", lockKey);
        }
    }
    
    /**
     * Executes a void task while holding a distributed lock.
     * 
     * @param lockKey the unique key for the lock
     * @param task the task to execute while holding the lock
     * @return true if lock was acquired and task executed
     */
    public boolean executeWithLock(String lockKey, Runnable task) {
        return executeWithLock(lockKey, getDefaultLockTTL(), task);
    }
    
    /**
     * Executes a void task while holding a distributed lock with custom TTL.
     * 
     * @param lockKey the unique key for the lock
     * @param ttl the time-to-live for the lock
     * @param task the task to execute while holding the lock
     * @return true if lock was acquired and task executed
     */
    public boolean executeWithLock(String lockKey, Duration ttl, Runnable task) {
        boolean acquired = coordinationService.tryAcquireLock(lockKey, ttl);
        
        if (!acquired) {
            log.debug("Failed to acquire lock: {} - another node holds it", lockKey);
            return false;
        }
        
        try {
            log.debug("Executing task with lock: {}", lockKey);
            task.run();
            return true;
        } catch (Exception e) {
            log.error("Error executing task with lock: {}", lockKey, e);
            throw e;
        } finally {
            coordinationService.releaseLock(lockKey);
            log.debug("Released lock: {}", lockKey);
        }
    }
    
    /**
     * Tries to acquire a lock without executing a task.
     * 
     * Caller is responsible for releasing the lock.
     * 
     * @param lockKey the unique key for the lock
     * @return true if lock was acquired
     */
    public boolean tryAcquire(String lockKey) {
        return coordinationService.tryAcquireLock(lockKey, getDefaultLockTTL());
    }
    
    /**
     * Tries to acquire a lock with custom TTL.
     * 
     * @param lockKey the unique key for the lock
     * @param ttl the time-to-live for the lock
     * @return true if lock was acquired
     */
    public boolean tryAcquire(String lockKey, Duration ttl) {
        return coordinationService.tryAcquireLock(lockKey, ttl);
    }
    
    /**
     * Releases a lock.
     *
     * @param lockKey the unique key for the lock
     */
    public void release(String lockKey) {
        coordinationService.releaseLock(lockKey);
    }

    /**
     * Renews a lock to extend its TTL.
     *
     * Used for long-running jobs that exceed the initial lock TTL.
     *
     * @param lockKey the unique key for the lock
     * @return true if renewal succeeded
     */
    public boolean renew(String lockKey) {
        return coordinationService.renewLock(lockKey, getDefaultLockTTL());
    }

    /**
     * Renews a lock with custom TTL.
     *
     * @param lockKey the unique key for the lock
     * @param ttl the new time-to-live for the lock
     * @return true if renewal succeeded
     */
    public boolean renew(String lockKey, Duration ttl) {
        return coordinationService.renewLock(lockKey, ttl);
    }

    /**
     * Checks if a lock is currently held.
     *
     * @param lockKey the unique key for the lock
     * @return true if the lock is held
     */
    public boolean isHeld(String lockKey) {
        return coordinationService.isLockHeld(lockKey);
    }

    /**
     * Gets the remaining TTL for a lock.
     *
     * @param lockKey the unique key for the lock
     * @return Optional containing the remaining TTL
     */
    public Optional<Duration> getRemainingTTL(String lockKey) {
        return coordinationService.getLockTTL(lockKey);
    }

    /**
     * Creates a lock key for a job.
     *
     * Format: "job:{jobId}"
     *
     * @param jobId the job ID
     * @return the lock key
     */
    public String createJobLockKey(Long jobId) {
        return String.format("job:%d", jobId);
    }

    /**
     * Creates a lock key for a job execution.
     *
     * Format: "execution:{executionId}"
     *
     * @param executionId the execution ID
     * @return the lock key
     */
    public String createExecutionLockKey(Long executionId) {
        return String.format("execution:%d", executionId);
    }

    /**
     * Gets the default lock TTL from configuration.
     *
     * @return the default lock TTL
     */
    private Duration getDefaultLockTTL() {
        return Duration.ofSeconds(properties.getDistributedLock().getLockTtlSeconds());
    }
}

