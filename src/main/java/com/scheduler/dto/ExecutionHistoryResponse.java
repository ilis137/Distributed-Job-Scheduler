package com.scheduler.dto;

import java.util.List;

/**
 * Paginated response for execution history.
 * 
 * Interview Talking Points:
 * - "Execution history can grow large, so pagination is essential"
 * - "Allows filtering by job, status, time range for debugging"
 * - "Provides observability into job execution patterns"
 * 
 * @param executions List of executions in current page
 * @param totalElements Total number of executions across all pages
 * @param totalPages Total number of pages
 * @param currentPage Current page number (0-indexed)
 * @param pageSize Number of items per page
 * @param hasNext Whether there are more pages
 * @param hasPrevious Whether there are previous pages
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record ExecutionHistoryResponse(
    List<JobExecutionResponse> executions,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
}

