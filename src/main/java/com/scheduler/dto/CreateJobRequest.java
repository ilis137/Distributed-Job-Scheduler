package com.scheduler.dto;

import com.scheduler.validation.ValidCronExpression;
import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new job.
 *
 * Uses Java 21 Record for immutability and conciseness.
 *
 * Interview Talking Points:
 * - "I use Records for DTOs because they're immutable, thread-safe, and reduce boilerplate"
 * - "Bean Validation annotations ensure data integrity at the API boundary"
 * - "Custom @ValidCronExpression validator uses Spring's CronExpression parser for accuracy"
 * - "Validation happens at the API boundary before any business logic executes"
 *
 * Design Decision:
 * "I use a custom @ValidCronExpression validator instead of regex because:
 * 1. Cron expressions have complex rules that are difficult to express in regex
 * 2. The validator uses the same Spring CronExpression parser as the scheduling logic
 * 3. This ensures validation consistency - if it passes here, it will parse during execution
 * 4. The validator provides detailed error messages for specific parsing failures"
 *
 * @param name Unique job name (alphanumeric, hyphens, underscores only)
 * @param description Optional job description
 * @param cronExpression Cron expression for scheduling (null for one-time jobs, validated by @ValidCronExpression)
 * @param payload Job payload (JSON or other format)
 * @param maxRetries Maximum retry attempts (0-10)
 * @param timeoutSeconds Job execution timeout in seconds (10-3600)
 * @param enabled Whether job is enabled for execution
 *
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record CreateJobRequest(

    @NotBlank(message = "Job name is required")
    @Size(min = 3, max = 100, message = "Job name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$",
             message = "Job name can only contain alphanumeric characters, hyphens, and underscores")
    String name,

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
    
    /**
     * Compact constructor with default values.
     * 
     * Interview Talking Point:
     * "Records support compact constructors for validation and default values.
     * This ensures DTOs are always in a valid state."
     */
    public CreateJobRequest {
        // Apply defaults if null
        if (maxRetries == null) {
            maxRetries = 3;
        }
        if (timeoutSeconds == null) {
            timeoutSeconds = 300; // 5 minutes default
        }
        if (enabled == null) {
            enabled = true;
        }
    }
}

