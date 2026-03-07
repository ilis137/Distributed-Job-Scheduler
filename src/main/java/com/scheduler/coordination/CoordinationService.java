package com.scheduler.coordination;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstraction for distributed coordination primitives.
 * 
 * This interface decouples the application from the specific coordination mechanism
 * (Redis, Zookeeper, etcd, etc.), making it easier to test and swap implementations.
 * 
 * Interview Talking Points:
 * - "I abstracted coordination primitives behind an interface for testability and flexibility"
 * - "This follows the Dependency Inversion Principle - high-level modules don't depend on low-level details"
 * - "I can easily mock this interface in unit tests without requiring Redis"
 * - "If we need stronger consistency guarantees, we could swap Redis for Zookeeper without changing business logic"
 * 
 * Design Decision:
 * "I chose Redis (AP system) over Zookeeper (CP system) for coordination because:
 * 1. Availability is more important than strong consistency for job scheduling
 * 2. Fencing tokens provide safety even if Redis has split-brain
 * 3. Redis is simpler to operate and has lower latency
 * 4. For a job scheduler, it's acceptable to have brief periods without a leader during partitions"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public interface CoordinationService {
    
    /**
     * Attempts to acquire leadership for the specified node.
     * 
     * This operation is atomic and uses a TTL-based lease. If the lease expires
     * without renewal, leadership is automatically released.
     * 
     * Interview Talking Point:
     * "I use TTL-based leases for leader election. The leader must renew the lease
     * via heartbeats at 1/3 TTL interval. If the leader crashes or is partitioned,
     * the lease expires and followers can compete for leadership."
     * 
     * @param nodeId the unique identifier of the node attempting to become leader
     * @param ttl the time-to-live for the leadership lease
     * @return true if leadership was acquired, false if another node is already leader
     */
    boolean tryAcquireLeadership(String nodeId, Duration ttl);
    
    /**
     * Renews the leadership lease for the specified node.
     * 
     * This must be called periodically (recommended: every TTL/3) to maintain leadership.
     * If renewal fails, the node should assume it has lost leadership.
     * 
     * Interview Talking Point:
     * "Heartbeat renewal is critical for failure detection. I renew at 1/3 TTL interval
     * to allow for 2 missed heartbeats before lease expiry. This balances fast failover
     * (short TTL) with tolerance for transient network issues."
     * 
     * @param nodeId the unique identifier of the current leader
     * @return true if renewal succeeded, false if leadership was lost
     */
    boolean renewLeadership(String nodeId);
    
    /**
     * Explicitly releases leadership for the specified node.
     * 
     * This should be called during graceful shutdown to allow immediate failover
     * rather than waiting for TTL expiry.
     * 
     * @param nodeId the unique identifier of the current leader
     */
    void releaseLeadership(String nodeId);
    
    /**
     * Gets the current leader node ID, if any.
     * 
     * @return Optional containing the leader node ID, or empty if no leader exists
     */
    Optional<String> getCurrentLeader();
    
    /**
     * Checks if the specified node is currently the leader.
     * 
     * @param nodeId the node ID to check
     * @return true if the node is the current leader
     */
    boolean isLeader(String nodeId);
    
    /**
     * Acquires a distributed lock with the specified key.
     * 
     * This implements the Redlock algorithm for distributed mutual exclusion.
     * The lock is automatically released after the TTL expires.
     * 
     * Interview Talking Point:
     * "I use the Redlock algorithm for distributed locking. The lock has a TTL
     * to prevent deadlocks if a node crashes while holding the lock. For long-running
     * jobs, I implement lock renewal to prevent premature expiry."
     * 
     * @param lockKey the unique key for the lock
     * @param ttl the time-to-live for the lock
     * @return true if lock was acquired, false if already held by another node
     */
    boolean tryAcquireLock(String lockKey, Duration ttl);
    
    /**
     * Renews a distributed lock to extend its TTL.
     * 
     * This is used for long-running jobs that exceed the initial lock TTL.
     * 
     * @param lockKey the unique key for the lock
     * @param ttl the new time-to-live for the lock
     * @return true if renewal succeeded, false if lock was lost
     */
    boolean renewLock(String lockKey, Duration ttl);
    
    /**
     * Releases a distributed lock.
     * 
     * @param lockKey the unique key for the lock
     */
    void releaseLock(String lockKey);
    
    /**
     * Checks if a lock is currently held.
     * 
     * @param lockKey the unique key for the lock
     * @return true if the lock is currently held
     */
    boolean isLockHeld(String lockKey);
    
    /**
     * Gets the remaining TTL for a lock.
     * 
     * @param lockKey the unique key for the lock
     * @return Optional containing the remaining TTL, or empty if lock doesn't exist
     */
    Optional<Duration> getLockTTL(String lockKey);
}

