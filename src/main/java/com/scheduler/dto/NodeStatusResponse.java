package com.scheduler.dto;

import com.scheduler.domain.enums.NodeRole;

import java.time.Instant;

/**
 * Response DTO for scheduler node status.
 * 
 * Interview Talking Points:
 * - "Node status shows health of each scheduler instance"
 * - "Epoch number indicates leader election generation (for fencing)"
 * - "Last heartbeat helps detect stale/crashed nodes"
 * 
 * @param nodeId Unique node identifier
 * @param role Node role (LEADER or FOLLOWER)
 * @param healthy Whether node is healthy (recent heartbeat)
 * @param epoch Current epoch number (incremented on leader election)
 * @param lastHeartbeat Timestamp of last heartbeat
 * @param startTime Node start time
 * @param version Application version
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record NodeStatusResponse(
    String nodeId,
    NodeRole role,
    boolean healthy,
    Long epoch,
    Instant lastHeartbeat,
    Instant startTime,
    String version
) {
}

