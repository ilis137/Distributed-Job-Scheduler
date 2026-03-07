-- ============================================================================
-- Flyway Migration: V3__create_scheduler_nodes_table.sql
-- Description: Creates the scheduler_nodes table - tracks cluster membership
-- Author: Distributed Job Scheduler Team
-- Date: 2026-03-07
-- ============================================================================

-- Interview Talking Points:
-- - Tracks node health and leadership status for observability
-- - Epoch numbers are CRITICAL for fencing token generation
-- - Heartbeat tracking enables failure detection
-- - Provides complete cluster state history for debugging

CREATE TABLE scheduler_nodes (
    -- Primary Key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Node Identification
    node_id VARCHAR(100) NOT NULL UNIQUE,
    hostname VARCHAR(255),
    port INT,
    
    -- Node Role
    role VARCHAR(20) NOT NULL DEFAULT 'INITIALIZING',
    
    -- Epoch Number (CRITICAL for Fencing)
    -- Interview: "Epoch number increments on each leader election - core of fencing mechanism"
    epoch BIGINT NOT NULL DEFAULT 0,
    
    -- Leadership Tracking
    became_leader_at TIMESTAMP NULL,
    
    -- Health Monitoring
    last_heartbeat_at TIMESTAMP NULL,
    healthy BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Metadata
    version VARCHAR(50),
    metadata TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_scheduler_nodes_node_id (node_id),
    INDEX idx_scheduler_nodes_role (role),
    INDEX idx_scheduler_nodes_heartbeat (last_heartbeat_at),
    INDEX idx_scheduler_nodes_role_healthy (role, healthy),
    INDEX idx_scheduler_nodes_epoch (epoch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks scheduler nodes in the distributed cluster';

-- Add column comments for documentation
ALTER TABLE scheduler_nodes MODIFY COLUMN node_id VARCHAR(100) NOT NULL 
    COMMENT 'Unique identifier for this node (hostname-UUID)';
    
ALTER TABLE scheduler_nodes MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'INITIALIZING' 
    COMMENT 'Node role (LEADER, FOLLOWER, INITIALIZING, SHUTTING_DOWN)';
    
ALTER TABLE scheduler_nodes MODIFY COLUMN epoch BIGINT NOT NULL DEFAULT 0 
    COMMENT 'Epoch number - incremented each time this node becomes leader';
    
ALTER TABLE scheduler_nodes MODIFY COLUMN last_heartbeat_at TIMESTAMP NULL 
    COMMENT 'Last heartbeat timestamp for failure detection';

