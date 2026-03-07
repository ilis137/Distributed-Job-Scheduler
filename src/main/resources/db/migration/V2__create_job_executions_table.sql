-- ============================================================================
-- Flyway Migration: V2__create_job_executions_table.sql
-- Description: Creates the job_executions table - tracks execution history
-- Author: Distributed Job Scheduler Team
-- Date: 2026-03-07
-- ============================================================================

-- Interview Talking Points:
-- - Fencing tokens prevent split-brain corruption in distributed systems
-- - Complete audit trail of all execution attempts for debugging
-- - Tracks execution duration for performance monitoring
-- - Foreign key with CASCADE DELETE maintains referential integrity

CREATE TABLE job_executions (
    -- Primary Key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Foreign Key to Job
    job_id BIGINT NOT NULL,
    
    -- Fencing Token (CRITICAL for Distributed Systems)
    -- Interview: "Fencing tokens with epoch numbers prevent zombie leaders from corrupting state"
    fencing_token VARCHAR(50) NOT NULL,
    
    -- Node Information
    node_id VARCHAR(50) NOT NULL,
    
    -- Execution Status
    status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    
    -- Timing Information
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    duration_ms BIGINT NULL,
    
    -- Error Information
    error_message VARCHAR(1000),
    error_stack_trace TEXT,
    
    -- Result
    result TEXT,
    
    -- Retry Information
    retry_attempt INT NOT NULL DEFAULT 0,
    
    -- Foreign Key Constraint
    CONSTRAINT fk_job_execution_job 
        FOREIGN KEY (job_id) REFERENCES jobs(id) 
        ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_job_executions_job_id (job_id),
    INDEX idx_job_executions_job_created (job_id, created_at),
    INDEX idx_job_executions_fencing_token (fencing_token),
    INDEX idx_job_executions_node_id (node_id),
    INDEX idx_job_executions_status (status),
    INDEX idx_job_executions_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores execution history for all job attempts';

-- Add column comments for documentation
ALTER TABLE job_executions MODIFY COLUMN fencing_token VARCHAR(50) NOT NULL 
    COMMENT 'Fencing token (epoch{N}-node{ID}) to prevent split-brain corruption';
    
ALTER TABLE job_executions MODIFY COLUMN node_id VARCHAR(50) NOT NULL 
    COMMENT 'ID of the scheduler node that executed this job';
    
ALTER TABLE job_executions MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'STARTED' 
    COMMENT 'Execution status (STARTED, SUCCESS, FAILED, TIMEOUT, CANCELLED, SKIPPED)';
    
ALTER TABLE job_executions MODIFY COLUMN duration_ms BIGINT NULL 
    COMMENT 'Execution duration in milliseconds';

