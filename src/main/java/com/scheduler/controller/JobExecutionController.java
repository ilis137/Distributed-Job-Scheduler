package com.scheduler.controller;

import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.enums.ExecutionStatus;
import com.scheduler.dto.DtoMapper;
import com.scheduler.dto.ExecutionHistoryResponse;
import com.scheduler.dto.JobExecutionResponse;
import com.scheduler.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for job execution history.
 * 
 * Provides read-only access to execution records for monitoring and debugging.
 * 
 * Interview Talking Points:
 * - "Execution history provides complete audit trail for debugging"
 * - "Pagination is essential because execution history grows unbounded"
 * - "Filtering by job, status, and time range helps narrow down issues"
 * - "Read-only endpoints can be scaled independently from write endpoints"
 * 
 * Design Decision:
 * "Execution records are immutable once created - no update or delete endpoints.
 * This preserves the audit trail and prevents tampering with execution history."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Slf4j
public class JobExecutionController {
    
    private final JobExecutionService jobExecutionService;
    
    /**
     * Gets execution details by ID.
     * 
     * @param id Execution ID
     * @return 200 OK with execution details, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobExecutionResponse> getExecution(@PathVariable Long id) {
        log.debug("Fetching execution: id={}", id);
        
        JobExecution execution = jobExecutionService.findById(id);
        JobExecutionResponse response = DtoMapper.toJobExecutionResponse(execution);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets execution history for a specific job.
     * 
     * Interview Talking Point:
     * "Job execution history shows all attempts, retries, and failures.
     * This is critical for debugging why a job keeps failing or for
     * analyzing performance trends over time."
     * 
     * @param jobId Job ID
     * @param pageable Pagination parameters
     * @return 200 OK with paginated execution history
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ExecutionHistoryResponse> getJobExecutionHistory(
            @PathVariable Long jobId,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Fetching execution history for job: jobId={}, page={}, size={}",
            jobId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<JobExecution> page = jobExecutionService.findByJobId(jobId, pageable);
        ExecutionHistoryResponse response = DtoMapper.toExecutionHistoryResponse(page);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lists all executions with optional filtering.
     * 
     * Interview Talking Point:
     * "Global execution history allows operators to see all job activity
     * across the cluster. Filtering by status helps identify patterns like
     * high failure rates or timeout issues."
     * 
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return 200 OK with paginated execution list
     */
    @GetMapping
    public ResponseEntity<ExecutionHistoryResponse> listExecutions(
            @RequestParam(required = false) ExecutionStatus status,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Listing executions: status={}, page={}, size={}",
            status, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<JobExecution> page = (status != null)
            ? jobExecutionService.findByStatus(status, pageable)
            : jobExecutionService.findAll(pageable);
        
        ExecutionHistoryResponse response = DtoMapper.toExecutionHistoryResponse(page);
        
        return ResponseEntity.ok(response);
    }
}

