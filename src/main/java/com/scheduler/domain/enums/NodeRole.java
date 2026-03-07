package com.scheduler.domain.enums;

/**
 * Represents the role of a scheduler node in the distributed cluster.
 * 
 * Interview Talking Point:
 * "I use a single-leader architecture where only the leader executes jobs.
 * Followers are on standby for automatic failover. This simplifies consistency
 * but creates a throughput bottleneck. For higher throughput, I could partition
 * jobs across multiple leaders using consistent hashing."
 */
public enum NodeRole {
    
    /**
     * Node is the current leader and actively executing jobs.
     * Only one node should be LEADER at any given time.
     * 
     * Responsibilities:
     * - Poll database for due jobs
     * - Acquire distributed locks
     * - Execute jobs in virtual thread pool
     * - Maintain heartbeat to renew leadership lease
     */
    LEADER,
    
    /**
     * Node is a follower on standby for failover.
     * Does not execute jobs but monitors leader health.
     * 
     * Responsibilities:
     * - Monitor leader heartbeat
     * - Attempt to acquire leadership if leader fails
     * - Serve read-only API requests (optional optimization)
     */
    FOLLOWER,
    
    /**
     * Node is starting up and determining its role.
     * Transitional state during initialization.
     */
    INITIALIZING,
    
    /**
     * Node is shutting down gracefully.
     * Draining in-flight jobs before stopping.
     * 
     * Interview Talking Point:
     * "Graceful shutdown is critical in distributed systems. I drain in-flight
     * jobs and release leadership before stopping to prevent job interruption."
     */
    SHUTTING_DOWN;
    
    /**
     * Checks if this role can execute jobs.
     * 
     * @return true if this role is allowed to execute jobs
     */
    public boolean canExecuteJobs() {
        return this == LEADER;
    }
    
    /**
     * Checks if this role should attempt leader election.
     * 
     * @return true if this role should compete for leadership
     */
    public boolean shouldAttemptElection() {
        return this == FOLLOWER || this == INITIALIZING;
    }
    
    /**
     * Checks if this role is in a stable state (not transitioning).
     * 
     * @return true if role is stable
     */
    public boolean isStable() {
        return this == LEADER || this == FOLLOWER;
    }
}

