package com.scheduler.dto;

import com.scheduler.domain.enums.JobStatus;

import java.time.Instant;

/**
 * Response DTO for job information.
 * 
 * Interview Talking Points:
 * - "Response DTOs decouple internal domain model from API contract"
 * - "This allows us to evolve the domain model without breaking API clients"
 * - "Records provide automatic equals/hashCode/toString for testing"
 * 
 * @param id Job ID
 * @param name Job name
 * @param description Job description
 * @param status Current job status
 * @param cronExpression Cron expression for scheduling
 * @param nextRunTime Next scheduled execution time
 * @param retryCount Number of times this job has been retried
 * @param maxRetries Maximum retry attempts
 * @param timeoutSeconds Job execution timeout in seconds
 * @param enabled Whether job is enabled
 * @param createdAt Job creation timestamp
 * @param updatedAt Last update timestamp
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record JobResponse(
    Long id,
    String name,
    String description,
    JobStatus status,
    String cronExpression,
    Instant nextRunTime,
    Integer retryCount,
    Integer maxRetries,
    Integer timeoutSeconds,
    Boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
}

