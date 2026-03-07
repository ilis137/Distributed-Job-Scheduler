package com.scheduler.domain.entity;

import com.scheduler.domain.enums.JobStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Core domain entity representing a scheduled job.
 * 
 * Interview Talking Points:
 * - Uses JPA directly in domain layer (pragmatic for this scale, no separate persistence layer)
 * - Optimistic locking with @Version to handle concurrent updates
 * - Domain logic methods for state transitions and validation
 * - Audit fields for debugging and compliance
 * 
 * Design Decision:
 * "I chose to use JPA entities directly in the domain layer rather than separating
 * domain and persistence entities. For a system of this scale, the added complexity
 * of mapping between layers doesn't provide enough benefit. If the domain logic
 * becomes more complex, I could refactor to separate them."
 */
@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_status_next_run", columnList = "status, next_run_time"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "payload") // Exclude potentially large payload from toString
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Optimistic locking version field.
     * 
     * Interview Talking Point:
     * "I use optimistic locking to handle concurrent updates. If two nodes try to
     * update the same job simultaneously, one will fail with OptimisticLockException.
     * This is better than pessimistic locking for read-heavy workloads."
     */
    @Version
    private Long version;
    
    @NotBlank(message = "Job name is required")
    @Size(min = 3, max = 100, message = "Job name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Job name can only contain alphanumeric characters, hyphens, and underscores")
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Size(max = 500)
    @Column(length = 500)
    private String description;
    
    /**
     * Cron expression for scheduling (e.g., "0 0 * * *" for daily at midnight).
     * Null for one-time jobs.
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;
    
    /**
     * Next scheduled execution time.
     * Calculated from cron expression or set explicitly for one-time jobs.
     * 
     * Interview Talking Point:
     * "The leader polls for jobs where next_run_time <= NOW() and status = PENDING.
     * I added a composite index on (status, next_run_time) for efficient polling."
     */
    @Column(name = "next_run_time")
    private Instant nextRunTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;
    
    /**
     * Job payload (JSON or other format).
     * Contains the actual work to be performed.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    /**
     * Maximum number of retry attempts for failed executions.
     */
    @Min(0)
    @Max(10)
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * Current retry attempt count.
     */
    @Min(0)
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Timeout in seconds for job execution.
     * If execution exceeds this, it will be marked as TIMEOUT.
     */
    @Min(1)
    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 300; // 5 minutes default
    
    /**
     * Whether this is a recurring job (has cron expression).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean recurring = false;
    
    /**
     * Whether this job is enabled.
     * Disabled jobs are not executed.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Last execution time (for tracking).
     */
    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;
    
    // ==================== Domain Logic Methods ====================
    
    /**
     * Validates if the job can transition to the target status.
     * 
     * @param targetStatus the status to transition to
     * @return true if transition is valid
     */
    public boolean canTransitionTo(JobStatus targetStatus) {
        return this.status.canTransitionTo(targetStatus);
    }
    
    /**
     * Transitions the job to a new status with validation.
     * 
     * @param targetStatus the status to transition to
     * @throws IllegalStateException if transition is invalid
     */
    public void transitionTo(JobStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                String.format("Invalid state transition from %s to %s for job %s",
                    this.status, targetStatus, this.name)
            );
        }
        this.status = targetStatus;
    }
    
    /**
     * Checks if the job should be retried after a failure.
     * 
     * @return true if retry attempts remain
     */
    public boolean shouldRetry() {
        return this.retryCount < this.maxRetries;
    }
    
    /**
     * Increments the retry count.
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    /**
     * Resets retry count (for recurring jobs after successful execution).
     */
    public void resetRetryCount() {
        this.retryCount = 0;
    }
    
    /**
     * Checks if the job is due for execution.
     * 
     * @return true if next_run_time is in the past or now
     */
    public boolean isDue() {
        return this.nextRunTime != null && 
               !this.nextRunTime.isAfter(Instant.now()) &&
               this.enabled &&
               this.status == JobStatus.PENDING;
    }
}

