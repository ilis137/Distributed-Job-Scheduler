package com.scheduler.dto;

import com.scheduler.domain.enums.ExecutionStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Response DTO for job execution information.
 * 
 * Interview Talking Points:
 * - "Execution records provide complete audit trail for debugging"
 * - "Fencing token shows which leader executed the job (split-brain detection)"
 * - "Duration calculation helps identify performance bottlenecks"
 * 
 * @param id Execution ID
 * @param jobId Associated job ID
 * @param jobName Associated job name
 * @param status Execution status
 * @param nodeId Node that executed the job
 * @param startTime Execution start time
 * @param endTime Execution end time
 * @param durationMs Execution duration in milliseconds
 * @param errorMessage Error message if execution failed
 * @param errorStackTrace Full stack trace if execution failed
 * @param fencingToken Fencing token used for this execution
 * @param retryAttempt Retry attempt number (0 for first attempt)
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record JobExecutionResponse(
    Long id,
    Long jobId,
    String jobName,
    ExecutionStatus status,
    String nodeId,
    Instant startTime,
    Instant endTime,
    Long durationMs,
    String errorMessage,
    String errorStackTrace,
    String fencingToken,
    Integer retryAttempt
) {
    
    /**
     * Calculates duration from start and end times.
     * 
     * @param startTime Start time
     * @param endTime End time
     * @return Duration in milliseconds, or null if not completed
     */
    public static Long calculateDuration(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime).toMillis();
    }
}

