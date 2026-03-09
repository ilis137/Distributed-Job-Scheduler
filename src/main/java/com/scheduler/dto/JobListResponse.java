package com.scheduler.dto;

import java.util.List;

/**
 * Paginated response for job listings.
 * 
 * Interview Talking Points:
 * - "Pagination prevents overwhelming clients with large datasets"
 * - "Includes metadata for building pagination UI (total pages, current page)"
 * - "Follows standard REST pagination patterns"
 * 
 * @param jobs List of jobs in current page
 * @param totalElements Total number of jobs across all pages
 * @param totalPages Total number of pages
 * @param currentPage Current page number (0-indexed)
 * @param pageSize Number of items per page
 * @param hasNext Whether there are more pages
 * @param hasPrevious Whether there are previous pages
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record JobListResponse(
    List<JobResponse> jobs,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
}

