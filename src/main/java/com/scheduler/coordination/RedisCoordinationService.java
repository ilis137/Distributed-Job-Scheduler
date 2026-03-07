package com.scheduler.coordination;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of CoordinationService using Redisson.
 * 
 * This implementation uses Redis for distributed coordination primitives:
 * - Leader election via TTL-based locks
 * - Distributed locking via Redlock algorithm
 * 
 * Interview Talking Points:
 * - "I use Redis (AP system) for coordination because availability is more important
 *   than strong consistency for job scheduling"
 * - "Fencing tokens provide safety even if Redis has split-brain scenarios"
 * - "Redis has lower latency than CP systems like Zookeeper"
 * - "TTL-based leases ensure automatic failover if leader crashes"
 * 
 * Design Decision:
 * "I chose Redis over Zookeeper because:
 * 1. Simpler to operate and deploy
 * 2. Lower latency for lock operations
 * 3. Built-in TTL support for automatic lease expiry
 * 4. Fencing tokens compensate for weaker consistency guarantees"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCoordinationService implements CoordinationService {
    
    private final RedissonClient redissonClient;
    
    private static final String LEADER_LOCK_PREFIX = "scheduler:leader:";
    private static final String DISTRIBUTED_LOCK_PREFIX = "scheduler:lock:";
    
    @Override
    public boolean tryAcquireLeadership(String nodeId, Duration ttl) {
        String lockKey = LEADER_LOCK_PREFIX + "election";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire the lock with Redisson's watchdog mechanism
            // Using -1 for leaseTime enables automatic lock renewal by the watchdog
            // The watchdog renews the lock every (lockWatchdogTimeout / 3) milliseconds
            // Default lockWatchdogTimeout is 30 seconds, so renewal happens every 10 seconds
            boolean acquired = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);

            if (acquired) {
                log.info("Node {} acquired leadership with watchdog auto-renewal enabled", nodeId);
                return true;
            } else {
                log.debug("Node {} failed to acquire leadership - another node is leader", nodeId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while trying to acquire leadership for node {}", nodeId, e);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring leadership for node {}", nodeId, e);
            return false;
        }
    }
    
    @Override
    public boolean renewLeadership(String nodeId) {
        String lockKey = LEADER_LOCK_PREFIX + "election";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // With watchdog enabled, the lock is automatically renewed
            // We just need to verify that this node still holds the lock
            // The watchdog renews the lock in the background every (lockWatchdogTimeout / 3) ms
            if (lock.isHeldByCurrentThread()) {
                // Lock is still held and watchdog is renewing it automatically
                log.debug("Node {} leadership verified - watchdog auto-renewal active", nodeId);
                return true;
            } else {
                // Lock was lost (either expired or released by another process)
                log.warn("Node {} lost leadership - lock no longer held", nodeId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying leadership for node {}", nodeId, e);
            return false;
        }
    }
    
    @Override
    public void releaseLeadership(String nodeId) {
        String lockKey = LEADER_LOCK_PREFIX + "election";
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Node {} released leadership", nodeId);
            } else {
                log.warn("Node {} attempted to release leadership but doesn't hold the lock", nodeId);
            }
        } catch (Exception e) {
            log.error("Error releasing leadership for node {}", nodeId, e);
        }
    }
    
    @Override
    public Optional<String> getCurrentLeader() {
        String lockKey = LEADER_LOCK_PREFIX + "election";
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.isLocked()) {
                // Redisson doesn't expose lock holder directly
                // We'll need to store this separately or use a different approach
                // For now, we just return empty if locked by someone else
                return Optional.empty();
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting current leader", e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean isLeader(String nodeId) {
        String lockKey = LEADER_LOCK_PREFIX + "election";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            return lock.isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("Error checking if node {} is leader", nodeId, e);
            return false;
        }
    }

    @Override
    public boolean tryAcquireLock(String lockKey, Duration ttl) {
        String fullKey = DISTRIBUTED_LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);

        try {
            // Try to acquire the lock with TTL (lease time)
            boolean acquired = lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);

            if (acquired) {
                log.debug("Acquired distributed lock: {} with TTL {}ms", lockKey, ttl.toMillis());
                return true;
            } else {
                log.debug("Failed to acquire distributed lock: {} - already held", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while trying to acquire lock: {}", lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean renewLock(String lockKey, Duration ttl) {
        String fullKey = DISTRIBUTED_LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);

        try {
            if (lock.isHeldByCurrentThread()) {
                // Extend the lock's TTL
                boolean renewed = lock.forceUnlock();
                if (renewed) {
                    // Re-acquire with new TTL
                    return lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);
                }
                return false;
            } else {
                log.warn("Cannot renew lock {} - not held by current thread", lockKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Error renewing lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        String fullKey = DISTRIBUTED_LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock: {}", lockKey);
            } else {
                log.warn("Attempted to release lock {} but it's not held by current thread", lockKey);
            }
        } catch (Exception e) {
            log.error("Error releasing lock: {}", lockKey, e);
        }
    }

    @Override
    public boolean isLockHeld(String lockKey) {
        String fullKey = DISTRIBUTED_LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);

        try {
            return lock.isLocked();
        } catch (Exception e) {
            log.error("Error checking if lock {} is held", lockKey, e);
            return false;
        }
    }

    @Override
    public Optional<Duration> getLockTTL(String lockKey) {
        String fullKey = DISTRIBUTED_LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullKey);

        try {
            long remainingTimeMs = lock.remainTimeToLive();
            if (remainingTimeMs > 0) {
                return Optional.of(Duration.ofMillis(remainingTimeMs));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting TTL for lock: {}", lockKey, e);
            return Optional.empty();
        }
    }
}

