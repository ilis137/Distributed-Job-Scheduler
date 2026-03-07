-- ============================================================================
-- Flyway Migration: V4__create_additional_indexes.sql
-- Description: Creates additional performance indexes (if needed)
-- Author: Distributed Job Scheduler Team
-- Date: 2026-03-07
-- ============================================================================

-- Interview Talking Points:
-- - All critical indexes were created in V1-V3 migrations
-- - This migration is a placeholder for future index optimizations
-- - Indexes are added based on query patterns observed in production
-- - Composite indexes are ordered by selectivity (most selective column first)

-- Note: All essential indexes have already been created in V1-V3.
-- This file serves as a placeholder for future index additions.

-- Example of how to add indexes in the future:
-- CREATE INDEX idx_jobs_recurring_enabled ON jobs(recurring, enabled);
-- CREATE INDEX idx_job_executions_completed_at ON job_executions(completed_at);

-- For now, this migration is intentionally empty but valid.
-- Flyway requires at least one statement, so we'll add a comment to the jobs table.

ALTER TABLE jobs COMMENT = 'Stores job definitions and scheduling information - all indexes created';

