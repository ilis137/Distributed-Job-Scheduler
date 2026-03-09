package com.scheduler.controller;

import com.scheduler.domain.entity.Job;
import com.scheduler.domain.enums.JobStatus;
import com.scheduler.dto.*;
import com.scheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for job management operations.
 * 
 * Provides CRUD operations and job lifecycle management.
 * 
 * Interview Talking Points:
 * - "RESTful API design with proper HTTP verbs and status codes"
 * - "Pagination prevents overwhelming clients with large datasets"
 * - "Validation at API boundary ensures data integrity"
 * - "Stateless design allows horizontal scaling"
 * 
 * Design Decisions:
 * - "I use ResponseEntity<T> for explicit control over HTTP status codes"
 * - "Pageable allows clients to control page size and sorting"
 * - "@Valid triggers Bean Validation before method execution"
 * - "All endpoints are idempotent where possible (PUT, DELETE)"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    
    private final JobService jobService;
    
    /**
     * Creates a new job.
     * 
     * Interview Talking Point:
     * "POST returns 201 Created with Location header pointing to the new resource.
     * This follows REST best practices for resource creation."
     * 
     * @param request Job creation request
     * @return 201 Created with job details
     */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        log.info("Creating new job: {}", request.name());

        // Determine if job is recurring based on cron expression
        boolean isRecurring = request.cronExpression() != null && !request.cronExpression().isBlank();

        Job job = Job.builder()
            .name(request.name())
            .description(request.description())
            .cronExpression(request.cronExpression())
            .payload(request.payload())
            .maxRetries(request.maxRetries())
            .timeoutSeconds(request.timeoutSeconds())
            .enabled(request.enabled())
            .recurring(isRecurring)
            .status(JobStatus.PENDING)
            .nextRunTime(calculateNextRunTime(request.cronExpression()))
            .retryCount(0)
            .build();

        Job created = jobService.createJob(job);
        JobResponse response = DtoMapper.toJobResponse(created);

        log.info("Job created successfully: id={}, name={}, recurring={}",
            created.getId(), created.getName(), created.getRecurring());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Gets a job by ID.
     * 
     * @param id Job ID
     * @return 200 OK with job details, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        log.debug("Fetching job: id={}", id);
        
        Job job = jobService.findById(id);
        JobResponse response = DtoMapper.toJobResponse(job);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lists all jobs with pagination and optional filtering.
     * 
     * Interview Talking Point:
     * "Pagination is essential for scalability. I use Spring Data's Pageable
     * which provides page number, size, and sorting out of the box."
     * 
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return 200 OK with paginated job list
     */
    @GetMapping
    public ResponseEntity<JobListResponse> listJobs(
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Listing jobs: status={}, page={}, size={}", status, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Job> page = (status != null)
            ? jobService.findByStatus(status, pageable)
            : jobService.findAll(pageable);
        
        JobListResponse response = DtoMapper.toJobListResponse(page);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates an existing job.
     * 
     * Interview Talking Point:
     * "PUT is used for updates. I support partial updates (PATCH semantics)
     * where null fields mean 'no change' rather than 'set to null'."
     * 
     * @param id Job ID
     * @param request Update request
     * @return 200 OK with updated job details
     */
    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request) {
        
        log.info("Updating job: id={}", id);
        
        Job job = jobService.findById(id);
        
        // Apply partial updates
        if (request.description() != null) {
            job.setDescription(request.description());
        }
        if (request.cronExpression() != null) {
            job.setCronExpression(request.cronExpression());
            job.setNextRunTime(calculateNextRunTime(request.cronExpression()));
            // Update recurring flag based on cron expression
            job.setRecurring(request.cronExpression() != null && !request.cronExpression().isBlank());
        }
        if (request.payload() != null) {
            job.setPayload(request.payload());
        }
        if (request.maxRetries() != null) {
            job.setMaxRetries(request.maxRetries());
        }
        if (request.timeoutSeconds() != null) {
            job.setTimeoutSeconds(request.timeoutSeconds());
        }
        if (request.enabled() != null) {
            job.setEnabled(request.enabled());
        }
        
        Job updated = jobService.updateJob(job);
        JobResponse response = DtoMapper.toJobResponse(updated);
        
        log.info("Job updated successfully: id={}", id);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a job.
     *
     * Interview Talking Point:
     * "DELETE returns 204 No Content on success. The job is soft-deleted
     * (marked as inactive) rather than hard-deleted to preserve audit trail."
     *
     * @param id Job ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        log.info("Deleting job: id={}", id);

        jobService.deleteJob(id);

        log.info("Job deleted successfully: id={}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Manually triggers a job execution.
     *
     * Interview Talking Point:
     * "Manual trigger allows operators to run jobs on-demand for testing
     * or recovery scenarios. The job is scheduled immediately regardless
     * of its cron schedule."
     *
     * @param id Job ID
     * @return 202 Accepted (job scheduled for execution)
     */
    @PostMapping("/{id}/trigger")
    public ResponseEntity<JobResponse> triggerJob(@PathVariable Long id) {
        log.info("Manually triggering job: id={}", id);

        Job job = jobService.findById(id);

        // Schedule job for immediate execution
        job.setNextRunTime(Instant.now());
        job.setStatus(JobStatus.PENDING);

        Job updated = jobService.updateJob(job);
        JobResponse response = DtoMapper.toJobResponse(updated);

        log.info("Job triggered successfully: id={}", id);

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Pauses a job.
     *
     * @param id Job ID
     * @return 200 OK with updated job details
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable Long id) {
        log.info("Pausing job: id={}", id);

        Job job = jobService.pauseJob(id);
        JobResponse response = DtoMapper.toJobResponse(job);

        log.info("Job paused successfully: id={}", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Resumes a paused job.
     *
     * @param id Job ID
     * @return 200 OK with updated job details
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<JobResponse> resumeJob(@PathVariable Long id) {
        log.info("Resuming job: id={}", id);

        Job job = jobService.resumeJob(id);
        JobResponse response = DtoMapper.toJobResponse(job);

        log.info("Job resumed successfully: id={}", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Calculates next run time from cron expression.
     *
     * Uses Spring's CronExpression to parse and calculate the next occurrence.
     *
     * Interview Talking Point:
     * "I use Spring's CronExpression class for cron parsing because it's built into
     * Spring Framework 5.3+, avoiding external dependencies like Quartz. It supports
     * standard cron format and integrates seamlessly with Spring Boot."
     *
     * Design Decision:
     * "For jobs without cron expressions (one-time jobs), I return Instant.now() for
     * immediate execution. For recurring jobs, I parse the cron expression and calculate
     * the next occurrence from the current time. This ensures jobs execute at the
     * intended schedule, not immediately."
     *
     * @param cronExpression the cron expression (null/blank for immediate execution)
     * @return the next run time
     * @throws IllegalArgumentException if cron expression is invalid
     */
    private Instant calculateNextRunTime(String cronExpression) {
        // One-time jobs (no cron expression) execute immediately
        if (cronExpression == null || cronExpression.isBlank()) {
            log.debug("No cron expression provided - scheduling for immediate execution");
            return Instant.now();
        }

        try {
            // Parse cron expression using Spring's CronExpression
            org.springframework.scheduling.support.CronExpression cron =
                org.springframework.scheduling.support.CronExpression.parse(cronExpression);

            // Calculate next occurrence from current time
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime next = cron.next(now);

            if (next == null) {
                log.warn("Cron expression '{}' has no future occurrences - scheduling for immediate execution",
                    cronExpression);
                return Instant.now();
            }

            // Convert to Instant using system default timezone
            Instant nextRunTime = next.atZone(java.time.ZoneId.systemDefault()).toInstant();

            log.info("Calculated next run time from cron expression '{}': {}",
                cronExpression, nextRunTime);

            return nextRunTime;

        } catch (IllegalArgumentException e) {
            log.error("Invalid cron expression '{}': {}", cronExpression, e.getMessage());
            throw new IllegalArgumentException(
                "Invalid cron expression '" + cronExpression + "': " + e.getMessage(), e);
        }
    }
}

