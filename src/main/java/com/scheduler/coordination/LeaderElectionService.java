package com.scheduler.coordination;

import com.scheduler.config.SchedulerProperties;
import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.domain.enums.NodeRole;
import com.scheduler.repository.SchedulerNodeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for distributed leader election using Redis.
 * 
 * Implements TTL-based leader election with automatic failover:
 * - Leader acquires a lock with TTL in Redis
 * - Leader sends heartbeats to renew the lock
 * - If leader crashes, TTL expires and followers compete for leadership
 * - Epoch numbers prevent split-brain scenarios
 * 
 * Interview Talking Points:
 * - "I use TTL-based leases for leader election - if the leader crashes,
 *   the lease expires automatically and followers can compete for leadership"
 * - "Heartbeat interval is TTL/3 to allow for 2 missed heartbeats before failover"
 * - "Epoch numbers provide fencing tokens to prevent zombie leaders from
 *   corrupting state after network partitions"
 * - "I track leadership in both Redis (source of truth) and database (observability)"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderElectionService {
    
    private final CoordinationService coordinationService;
    private final SchedulerNodeRepository nodeRepository;
    private final SchedulerProperties properties;
    
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private ScheduledExecutorService scheduler;
    private SchedulerNode currentNode;
    
    /**
     * Initializes the leader election service.
     * 
     * Creates or updates the node record in the database and starts
     * the leader election process.
     */
    @PostConstruct
    public void initialize() {
        if (!properties.getLeaderElection().isEnabled()) {
            log.info("Leader election is disabled");
            return;
        }
        
        log.info("Initializing leader election for node: {}", properties.getNode().getId());
        
        // Initialize node in database
        currentNode = initializeNode();
        
        // Start leader election
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setName("leader-election");
            thread.setDaemon(true);
            return thread;
        });
        
        isRunning.set(true);
        
        // Start election attempts immediately
        scheduler.scheduleAtFixedRate(
            this::attemptLeaderElection,
            0,
            properties.getLeaderElection().getHeartbeatIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        log.info("Leader election service started for node: {}", properties.getNode().getId());
    }
    
    /**
     * Shuts down the leader election service gracefully.
     * 
     * Releases leadership if this node is the leader and stops the scheduler.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down leader election service for node: {}", properties.getNode().getId());
        
        isRunning.set(false);
        
        if (isLeader.get()) {
            releaseLeadership();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Mark node as unhealthy in database
        markNodeUnhealthy();
        
        log.info("Leader election service shut down for node: {}", properties.getNode().getId());
    }
    
    /**
     * Attempts to acquire or renew leadership.
     * 
     * If not leader: tries to acquire leadership
     * If leader: renews the leadership lease
     */
    private void attemptLeaderElection() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            String nodeId = properties.getNode().getId();
            Duration ttl = Duration.ofSeconds(properties.getLeaderElection().getLockTtlSeconds());
            
            if (isLeader.get()) {
                // Renew leadership
                boolean renewed = coordinationService.renewLeadership(nodeId);
                if (renewed) {
                    updateHeartbeat();
                } else {
                    log.warn("Failed to renew leadership - transitioning to follower");
                    transitionToFollower();
                }
            } else {
                // Try to become leader
                boolean acquired = coordinationService.tryAcquireLeadership(nodeId, ttl);
                if (acquired) {
                    transitionToLeader();
                }
            }
        } catch (Exception e) {
            log.error("Error during leader election", e);
        }
    }

    /**
     * Transitions this node to leader role.
     *
     * Increments epoch, updates database, and sets leader flag.
     */
    @Transactional
    protected void transitionToLeader() {
        log.info("Node {} transitioning to LEADER", properties.getNode().getId());

        isLeader.set(true);

        // Update node in database
        currentNode.promoteToLeader();
        nodeRepository.save(currentNode);

        log.info("Node {} is now LEADER with epoch {}",
            properties.getNode().getId(), currentNode.getEpoch());
    }

    /**
     * Transitions this node to follower role.
     *
     * Updates database and clears leader flag.
     */
    @Transactional
    protected void transitionToFollower() {
        log.info("Node {} transitioning to FOLLOWER", properties.getNode().getId());

        isLeader.set(false);

        // Update node in database
        currentNode.demoteToFollower();
        nodeRepository.save(currentNode);

        log.info("Node {} is now FOLLOWER", properties.getNode().getId());
    }

    /**
     * Releases leadership explicitly.
     *
     * Called during graceful shutdown.
     */
    @Transactional
    protected void releaseLeadership() {
        log.info("Node {} releasing leadership", properties.getNode().getId());

        coordinationService.releaseLeadership(properties.getNode().getId());
        transitionToFollower();
    }

    /**
     * Updates the heartbeat timestamp for this node.
     */
    @Transactional
    protected void updateHeartbeat() {
        currentNode.recordHeartbeat();
        nodeRepository.save(currentNode);
    }

    /**
     * Initializes the node record in the database.
     *
     * Creates a new node if it doesn't exist, or updates existing node.
     *
     * @return the initialized SchedulerNode
     */
    @Transactional
    protected SchedulerNode initializeNode() {
        String nodeId = properties.getNode().getId();

        Optional<SchedulerNode> existing = nodeRepository.findByNodeId(nodeId);

        if (existing.isPresent()) {
            SchedulerNode node = existing.get();
            node.setRole(NodeRole.FOLLOWER);
            node.setHealthy(true);
            node.recordHeartbeat();
            return nodeRepository.save(node);
        } else {
            SchedulerNode node = SchedulerNode.builder()
                .nodeId(nodeId)
                .role(NodeRole.FOLLOWER)
                .epoch(0L)
                .healthy(true)
                .version("1.0.0")
                .build();
            node.recordHeartbeat();
            return nodeRepository.save(node);
        }
    }

    /**
     * Marks the node as unhealthy in the database.
     */
    @Transactional
    protected void markNodeUnhealthy() {
        if (currentNode != null) {
            currentNode.markUnhealthy();
            nodeRepository.save(currentNode);
        }
    }

    /**
     * Checks if this node is currently the leader.
     *
     * @return true if this node is the leader
     */
    public boolean isLeader() {
        return isLeader.get();
    }

    /**
     * Gets the current epoch number for this node.
     *
     * @return the epoch number
     */
    public long getCurrentEpoch() {
        return currentNode != null ? currentNode.getEpoch() : 0L;
    }

    /**
     * Gets the current node entity.
     *
     * @return the SchedulerNode
     */
    public SchedulerNode getCurrentNode() {
        return currentNode;
    }
}

