package com.scheduler.dto;

import com.scheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing job.
 *
 * All fields are optional - only provided fields will be updated.
 *
 * Interview Talking Points:
 * - "Partial updates allow clients to modify only specific fields"
 * - "Null values indicate 'no change' rather than 'set to null'"
 * - "This follows the PATCH semantics rather than PUT (full replacement)"
 * - "Custom @ValidCronExpression validator ensures cron format consistency"
 *
 * @param description Optional job description
 * @param cronExpression Cron expression for scheduling (validated by @ValidCronExpression)
 * @param payload Job payload (JSON or other format)
 * @param maxRetries Maximum retry attempts (0-10)
 * @param timeoutSeconds Job execution timeout in seconds (10-3600)
 * @param enabled Whether job is enabled for execution
 *
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record UpdateJobRequest(

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    @ValidCronExpression(message = "Invalid cron expression format")
    String cronExpression,
    
    @Size(max = 10000, message = "Payload cannot exceed 10000 characters")
    String payload,
    
    @Min(value = 0, message = "Max retries must be at least 0")
    @Max(value = 10, message = "Max retries cannot exceed 10")
    Integer maxRetries,
    
    @Min(value = 10, message = "Timeout must be at least 10 seconds")
    @Max(value = 3600, message = "Timeout cannot exceed 3600 seconds (1 hour)")
    Integer timeoutSeconds,
    
    Boolean enabled
) {
}

