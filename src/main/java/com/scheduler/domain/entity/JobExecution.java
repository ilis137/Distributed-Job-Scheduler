package com.scheduler.domain.entity;

import com.scheduler.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a single execution attempt of a job.
 * 
 * Interview Talking Points:
 * - Fencing tokens prevent split-brain scenarios in distributed systems
 * - Complete audit trail of all execution attempts
 * - Tracks execution duration for performance monitoring
 * - Stores error details for debugging
 * 
 * Fencing Token Design:
 * "I use fencing tokens (epoch numbers) to prevent zombie leaders from corrupting
 * state after network partitions. Each leader election increments the epoch.
 * The database validates that writes come from the current epoch, rejecting
 * stale writes from old leaders."
 */
@Entity
@Table(name = "job_executions", indexes = {
    @Index(name = "idx_job_id_created", columnList = "job_id, created_at"),
    @Index(name = "idx_fencing_token", columnList = "fencing_token"),
    @Index(name = "idx_node_id", columnList = "node_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"errorMessage", "errorStackTrace"})
public class JobExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the job being executed.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    /**
     * Fencing token to prevent split-brain scenarios.
     * Format: "epoch{N}-node{ID}" (e.g., "epoch5-node1")
     * 
     * Interview Talking Point:
     * "The fencing token contains the epoch number from leader election.
     * If a network partition causes two leaders, only the one with the higher
     * epoch can write to the database. This prevents zombie leaders from
     * corrupting state."
     */
    @NotNull
    @Column(name = "fencing_token", nullable = false, length = 50)
    private String fencingToken;
    
    /**
     * ID of the scheduler node that executed this job.
     */
    @NotNull
    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.STARTED;
    
    /**
     * When the execution started.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * When the execution started (explicit field for clarity).
     */
    @Column(name = "started_at")
    private Instant startedAt;
    
    /**
     * When the execution completed (success or failure).
     */
    @Column(name = "completed_at")
    private Instant completedAt;
    
    /**
     * Duration of execution in milliseconds.
     * Calculated as (completedAt - startedAt).
     */
    @Column(name = "duration_ms")
    private Long durationMs;
    
    /**
     * Error message if execution failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * Full stack trace if execution failed.
     * Stored for debugging purposes.
     */
    @Lob
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;
    
    /**
     * Result of the execution (if any).
     * Could be JSON, plain text, or other format.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String result;
    
    /**
     * Retry attempt number (0 for first attempt, 1 for first retry, etc.).
     */
    @Column(name = "retry_attempt", nullable = false)
    @Builder.Default
    private Integer retryAttempt = 0;
    
    // ==================== Domain Logic Methods ====================
    
    /**
     * Marks the execution as completed with the given status.
     * Calculates duration automatically.
     * 
     * @param status the final execution status
     */
    public void complete(ExecutionStatus status) {
        this.status = status;
        this.completedAt = Instant.now();
        if (this.startedAt != null) {
            this.durationMs = Duration.between(this.startedAt, this.completedAt).toMillis();
        }
    }
    
    /**
     * Marks the execution as failed with error details.
     * 
     * @param errorMessage the error message
     * @param errorStackTrace the full stack trace
     */
    public void fail(String errorMessage, String errorStackTrace) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
        complete(ExecutionStatus.FAILED);
    }
    
    /**
     * Marks the execution as successful with optional result.
     * 
     * @param result the execution result
     */
    public void succeed(String result) {
        this.result = result;
        complete(ExecutionStatus.SUCCESS);
    }
    
    /**
     * Marks the execution as timed out.
     */
    public void timeout() {
        complete(ExecutionStatus.TIMEOUT);
    }
    
    /**
     * Checks if this execution was successful.
     * 
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return this.status == ExecutionStatus.SUCCESS;
    }
    
    /**
     * Checks if this execution should trigger a retry.
     * 
     * @return true if status indicates retry is needed
     */
    public boolean shouldRetry() {
        return this.status.shouldRetry();
    }
    
    /**
     * Extracts the epoch number from the fencing token.
     * 
     * Interview Talking Point:
     * "I can extract the epoch from the fencing token to validate that writes
     * come from the current leader. This is a simple but effective way to
     * prevent split-brain corruption."
     * 
     * @return the epoch number, or -1 if token format is invalid
     */
    public long getEpoch() {
        if (fencingToken == null || !fencingToken.startsWith("epoch")) {
            return -1;
        }
        try {
            String epochPart = fencingToken.substring(5, fencingToken.indexOf("-node"));
            return Long.parseLong(epochPart);
        } catch (Exception e) {
            return -1;
        }
    }
}

