package com.scheduler.repository;

import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.domain.enums.NodeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SchedulerNode entity.
 * 
 * Interview Talking Points:
 * - Tracks cluster membership and health
 * - Supports leader election monitoring
 * - Enables debugging of distributed system issues
 */
@Repository
public interface SchedulerNodeRepository extends JpaRepository<SchedulerNode, Long> {
    
    /**
     * Finds a node by its unique node ID.
     * 
     * @param nodeId the node ID
     * @return Optional containing the node if found
     */
    Optional<SchedulerNode> findByNodeId(String nodeId);
    
    /**
     * Finds all nodes with a specific role.
     * 
     * @param role the node role
     * @return list of nodes with the role
     */
    List<SchedulerNode> findByRole(NodeRole role);
    
    /**
     * Finds the current leader node.
     * 
     * Interview Talking Point:
     * "While Redis holds the source of truth for leadership (via TTL-based locks),
     * I also track it in the database for observability. This query should return
     * at most one node. If it returns multiple, it indicates a bug in the leader
     * election logic."
     * 
     * @return Optional containing the leader node
     */
    @Query("SELECT n FROM SchedulerNode n WHERE n.role = 'LEADER' AND n.healthy = true")
    Optional<SchedulerNode> findCurrentLeader();
    
    /**
     * Finds all healthy nodes.
     * 
     * @return list of healthy nodes
     */
    @Query("SELECT n FROM SchedulerNode n WHERE n.healthy = true ORDER BY n.createdAt")
    List<SchedulerNode> findHealthyNodes();
    
    /**
     * Finds all follower nodes that are healthy.
     * These are candidates for leader election.
     * 
     * @return list of healthy follower nodes
     */
    @Query("""
        SELECT n FROM SchedulerNode n
        WHERE n.role = 'FOLLOWER'
          AND n.healthy = true
        ORDER BY n.createdAt
        """)
    List<SchedulerNode> findHealthyFollowers();
    
    /**
     * Finds nodes with stale heartbeats.
     * These nodes may have crashed or become unreachable.
     * 
     * Interview Talking Point:
     * "I have a background task that periodically checks for nodes with stale
     * heartbeats. If a node hasn't sent a heartbeat in 30 seconds (3x the heartbeat
     * interval), I mark it as unhealthy. This helps with cluster monitoring."
     * 
     * @param threshold the time threshold (nodes with heartbeat older than this are stale)
     * @return list of nodes with stale heartbeats
     */
    @Query("""
        SELECT n FROM SchedulerNode n
        WHERE n.lastHeartbeatAt < :threshold
          AND n.healthy = true
        """)
    List<SchedulerNode> findNodesWithStaleHeartbeat(@Param("threshold") Instant threshold);
    
    /**
     * Finds the node with the highest epoch number.
     * This should be the current or most recent leader.
     * 
     * @return Optional containing the node with highest epoch
     */
    @Query("SELECT n FROM SchedulerNode n ORDER BY n.epoch DESC LIMIT 1")
    Optional<SchedulerNode> findNodeWithHighestEpoch();
    
    /**
     * Counts nodes by role.
     * 
     * @param role the node role
     * @return count of nodes with the role
     */
    long countByRole(NodeRole role);
    
    /**
     * Counts healthy nodes.
     * 
     * @return count of healthy nodes
     */
    long countByHealthy(boolean healthy);
}

