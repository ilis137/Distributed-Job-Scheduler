package com.scheduler.domain.entity;

import com.scheduler.domain.enums.NodeRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a scheduler node in the distributed cluster.
 * 
 * Interview Talking Points:
 * - Tracks cluster membership and node health
 * - Records leadership history with epoch numbers
 * - Enables monitoring and debugging of cluster state
 * - Supports graceful shutdown and failover
 * 
 * Design Decision:
 * "I track node state in the database for observability and debugging.
 * While Redis holds the source of truth for leadership (via TTL-based locks),
 * the database provides a historical record of leader elections and node health."
 */
@Entity
@Table(name = "scheduler_nodes", indexes = {
    @Index(name = "idx_node_id", columnList = "node_id", unique = true),
    @Index(name = "idx_role", columnList = "role"),
    @Index(name = "idx_last_heartbeat", columnList = "last_heartbeat_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SchedulerNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique identifier for this node.
     * Format: hostname-UUID or custom identifier.
     * 
     * Interview Talking Point:
     * "I use a combination of hostname and UUID to ensure node IDs are unique
     * even if multiple instances run on the same host (e.g., in Kubernetes)."
     */
    @NotBlank
    @Column(name = "node_id", nullable = false, unique = true, length = 100)
    private String nodeId;
    
    /**
     * Hostname or IP address of the node.
     */
    @Column(length = 255)
    private String hostname;
    
    /**
     * Port number the node is listening on.
     */
    @Column
    private Integer port;
    
    /**
     * Current role of the node in the cluster.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NodeRole role = NodeRole.INITIALIZING;
    
    /**
     * Current epoch number for this node.
     * Incremented each time this node becomes leader.
     * 
     * Interview Talking Point:
     * "The epoch number is the core of the fencing token mechanism.
     * Each leader election increments the epoch, and the database validates
     * that writes come from the current epoch. This prevents zombie leaders
     * from corrupting state after network partitions."
     */
    @Column(nullable = false)
    @Builder.Default
    private Long epoch = 0L;
    
    /**
     * When this node became leader (if currently leader).
     * Null if node is not leader.
     */
    @Column(name = "became_leader_at")
    private Instant becameLeaderAt;
    
    /**
     * When this node last sent a heartbeat.
     * Used to detect failed nodes.
     */
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
    
    /**
     * Whether this node is currently healthy.
     * Set to false during graceful shutdown or if heartbeat fails.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean healthy = true;
    
    /**
     * Version of the scheduler application running on this node.
     */
    @Column(length = 50)
    private String version;
    
    /**
     * Additional metadata about the node (JSON format).
     * Could include JVM version, OS, memory, etc.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // ==================== Domain Logic Methods ====================
    
    /**
     * Promotes this node to leader role.
     * Increments epoch and records leadership timestamp.
     * 
     * Interview Talking Point:
     * "When a node becomes leader, I increment its epoch number. This ensures
     * that each leader has a unique, monotonically increasing identifier that
     * can be used for fencing."
     */
    public void promoteToLeader() {
        this.role = NodeRole.LEADER;
        this.epoch++;
        this.becameLeaderAt = Instant.now();
        this.lastHeartbeatAt = Instant.now();
    }
    
    /**
     * Demotes this node to follower role.
     * Clears leadership timestamp but preserves epoch.
     */
    public void demoteToFollower() {
        this.role = NodeRole.FOLLOWER;
        this.becameLeaderAt = null;
    }
    
    /**
     * Records a heartbeat from this node.
     * Updates last heartbeat timestamp.
     */
    public void recordHeartbeat() {
        this.lastHeartbeatAt = Instant.now();
        this.healthy = true;
    }
    
    /**
     * Marks this node as unhealthy.
     * Called when heartbeat fails or during shutdown.
     */
    public void markUnhealthy() {
        this.healthy = false;
    }
    
    /**
     * Checks if this node is the current leader.
     * 
     * @return true if role is LEADER
     */
    public boolean isLeader() {
        return this.role == NodeRole.LEADER;
    }
    
    /**
     * Checks if this node's heartbeat is stale.
     * 
     * @param maxAgeSeconds maximum age in seconds before heartbeat is considered stale
     * @return true if heartbeat is older than maxAgeSeconds
     */
    public boolean isHeartbeatStale(long maxAgeSeconds) {
        if (lastHeartbeatAt == null) {
            return true;
        }
        Instant threshold = Instant.now().minusSeconds(maxAgeSeconds);
        return lastHeartbeatAt.isBefore(threshold);
    }
    
    /**
     * Generates a fencing token for this node.
     * Format: "epoch{N}-node{ID}"
     * 
     * @return the fencing token
     */
    public String generateFencingToken() {
        return String.format("epoch%d-node%s", this.epoch, this.nodeId);
    }
}

