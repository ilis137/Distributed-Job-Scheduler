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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for managing node heartbeats and failure detection.
 * 
 * Responsibilities:
 * - Send periodic heartbeats to update node health status
 * - Detect failed nodes based on stale heartbeats
 * - Clean up stale node records from the database
 * 
 * Interview Talking Points:
 * - "Heartbeats are critical for failure detection in distributed systems"
 * - "I send heartbeats at 1/3 of the TTL interval to allow for 2 missed heartbeats"
 * - "Stale nodes are detected by checking if heartbeat is older than 3x the interval"
 * - "I track heartbeats in the database for observability and debugging"
 * - "The heartbeat mechanism is separate from Redis TTL for redundancy"
 * 
 * Design Decision:
 * "I use both Redis TTL (for leader election) and database heartbeats (for observability).
 * Redis TTL is the source of truth for leadership, but database heartbeats provide
 * a historical record and enable monitoring of cluster health."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeartbeatService {
    
    private final SchedulerNodeRepository nodeRepository;
    private final SchedulerProperties properties;
    private final LeaderElectionService leaderElectionService;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    
    /**
     * Initializes the heartbeat service.
     * 
     * Starts periodic heartbeat sending and stale node detection.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing heartbeat service for node: {}", properties.getNode().getId());
        
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r);
            thread.setName("heartbeat-service");
            thread.setDaemon(true);
            return thread;
        });
        
        isRunning.set(true);
        
        // Send heartbeats at the configured interval
        int intervalSeconds = properties.getLeaderElection().getHeartbeatIntervalSeconds();
        scheduler.scheduleAtFixedRate(
            this::sendHeartbeat,
            0,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        
        // Detect stale nodes every 30 seconds
        scheduler.scheduleAtFixedRate(
            this::detectStaleNodes,
            30,
            30,
            TimeUnit.SECONDS
        );
        
        log.info("Heartbeat service started with interval: {}s", intervalSeconds);
    }
    
    /**
     * Shuts down the heartbeat service.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down heartbeat service");
        
        isRunning.set(false);
        
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
        
        log.info("Heartbeat service shut down");
    }
    
    /**
     * Sends a heartbeat for this node.
     * 
     * Updates the last_heartbeat_at timestamp in the database.
     */
    @Transactional
    protected void sendHeartbeat() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            String nodeId = properties.getNode().getId();
            SchedulerNode node = leaderElectionService.getCurrentNode();
            
            if (node != null) {
                node.recordHeartbeat();
                nodeRepository.save(node);
                
                log.debug("Heartbeat sent for node: {} (role: {}, epoch: {})",
                    nodeId, node.getRole(), node.getEpoch());
            } else {
                log.warn("Cannot send heartbeat - current node not initialized");
            }
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
        }
    }
    
    /**
     * Detects and marks stale nodes as unhealthy.
     * 
     * A node is considered stale if its heartbeat is older than 3x the heartbeat interval.
     */
    @Transactional
    protected void detectStaleNodes() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            int intervalSeconds = properties.getLeaderElection().getHeartbeatIntervalSeconds();
            long staleThresholdSeconds = intervalSeconds * 3L;
            
            List<SchedulerNode> healthyNodes = nodeRepository.findHealthyNodes();
            
            for (SchedulerNode node : healthyNodes) {
                if (node.isHeartbeatStale(staleThresholdSeconds)) {
                    log.warn("Detected stale node: {} (last heartbeat: {})",
                        node.getNodeId(), node.getLastHeartbeatAt());
                    
                    node.markUnhealthy();
                    
                    // If it was a leader, demote it
                    if (node.getRole() == NodeRole.LEADER) {
                        log.warn("Stale leader detected: {} - demoting to follower", node.getNodeId());
                        node.demoteToFollower();
                    }
                    
                    nodeRepository.save(node);
                }
            }
        } catch (Exception e) {
            log.error("Error detecting stale nodes", e);
        }
    }
}

