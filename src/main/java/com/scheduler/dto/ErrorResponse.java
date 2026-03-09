package com.scheduler.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response for API errors.
 * 
 * Interview Talking Points:
 * - "Consistent error format makes it easier for clients to handle errors"
 * - "Includes timestamp for correlation with logs"
 * - "Validation errors include field-level details for better UX"
 * 
 * @param timestamp When the error occurred
 * @param status HTTP status code
 * @param error HTTP status reason phrase
 * @param message Human-readable error message
 * @param path Request path that caused the error
 * @param validationErrors Field-level validation errors (if applicable)
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<ValidationError> validationErrors
) {
    
    /**
     * Creates error response without validation errors.
     */
    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
    
    /**
     * Field-level validation error.
     * 
     * @param field Field name that failed validation
     * @param rejectedValue Value that was rejected
     * @param message Validation error message
     */
    public record ValidationError(
        String field,
        Object rejectedValue,
        String message
    ) {
    }
}

