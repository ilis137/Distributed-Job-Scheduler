-- ============================================================================
-- Flyway Migration: V1__create_jobs_table.sql
-- Description: Creates the jobs table - core entity for job definitions
-- Author: Distributed Job Scheduler Team
-- Date: 2026-03-07
-- ============================================================================

-- Interview Talking Points:
-- - Composite index on (status, next_run_time) is CRITICAL for efficient job polling
-- - Optimistic locking with version column prevents concurrent update conflicts
-- - Unique constraint on name ensures idempotency
-- - Audit fields (created_at, updated_at) for tracking and debugging

CREATE TABLE jobs (
    -- Primary Key
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Optimistic Locking
    -- Interview: "I use optimistic locking to handle concurrent updates without deadlocks"
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Job Identification
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    
    -- Scheduling
    cron_expression VARCHAR(100),
    next_run_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Job Configuration
    payload TEXT,
    max_retries INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    timeout_seconds INT NOT NULL DEFAULT 300,
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_executed_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_jobs_status_next_run (status, next_run_time),
    INDEX idx_jobs_status (status),
    INDEX idx_jobs_created_at (created_at),
    INDEX idx_jobs_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores job definitions and scheduling information';

-- Add column comments for documentation
ALTER TABLE jobs MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0 
    COMMENT 'Optimistic locking version';
    
ALTER TABLE jobs MODIFY COLUMN next_run_time TIMESTAMP NULL 
    COMMENT 'Next scheduled execution time - critical for job polling';
    
ALTER TABLE jobs MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
    COMMENT 'Current job status (PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, RETRYING, DEAD_LETTER, PAUSED)';
    
ALTER TABLE jobs MODIFY COLUMN payload TEXT 
    COMMENT 'Job payload in JSON or other format';

