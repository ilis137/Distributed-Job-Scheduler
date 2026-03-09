package com.scheduler.repository;

import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for JobExecution entity.
 * 
 * Interview Talking Points:
 * - Tracks complete execution history for audit and debugging
 * - Supports fencing token validation
 * - Enables performance monitoring and failure analysis
 */
@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    
    /**
     * Finds all executions for a specific job, ordered by creation time.
     *
     * @param jobId the job ID
     * @return list of executions for the job
     */
    @Query("SELECT e FROM JobExecution e WHERE e.job.id = :jobId ORDER BY e.createdAt DESC")
    List<JobExecution> findByJobId(@Param("jobId") Long jobId);

    /**
     * Finds all executions for a specific job (paginated).
     *
     * @param jobId the job ID
     * @param pageable pagination parameters
     * @return page of executions for the job
     */
    @Query("SELECT e FROM JobExecution e WHERE e.job.id = :jobId ORDER BY e.startTime DESC")
    Page<JobExecution> findByJobId(@Param("jobId") Long jobId, Pageable pageable);
    
    /**
     * Finds the most recent execution for a job.
     * 
     * @param jobId the job ID
     * @return Optional containing the most recent execution
     */
    @Query("""
        SELECT e FROM JobExecution e
        WHERE e.job.id = :jobId
        ORDER BY e.createdAt DESC
        LIMIT 1
        """)
    Optional<JobExecution> findLatestByJobId(@Param("jobId") Long jobId);
    
    /**
     * Finds all executions by a specific node.
     * Useful for debugging node-specific issues.
     * 
     * @param nodeId the node ID
     * @return list of executions by the node
     */
    List<JobExecution> findByNodeId(String nodeId);
    
    /**
     * Finds all executions with a specific fencing token.
     * 
     * Interview Talking Point:
     * "I can query executions by fencing token to verify that only the current
     * leader is executing jobs. If I see executions from old epochs, it indicates
     * a split-brain scenario that the fencing mechanism should have prevented."
     * 
     * @param fencingToken the fencing token
     * @return list of executions with the token
     */
    List<JobExecution> findByFencingToken(String fencingToken);
    
    /**
     * Finds executions with a specific status.
     *
     * @param status the execution status
     * @return list of executions with the status
     */
    List<JobExecution> findByStatus(ExecutionStatus status);

    /**
     * Finds executions with a specific status (paginated).
     *
     * @param status the execution status
     * @param pageable pagination parameters
     * @return page of executions with the status
     */
    Page<JobExecution> findByStatus(ExecutionStatus status, Pageable pageable);
    
    /**
     * Finds failed executions within a time range.
     * Useful for failure analysis and alerting.
     * 
     * @param start start of time range
     * @param end end of time range
     * @return list of failed executions
     */
    @Query("""
        SELECT e FROM JobExecution e
        WHERE e.status IN ('FAILED', 'TIMEOUT')
          AND e.createdAt BETWEEN :start AND :end
        ORDER BY e.createdAt DESC
        """)
    List<JobExecution> findFailedExecutions(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Calculates average execution duration for a job.
     * Useful for performance monitoring.
     * 
     * @param jobId the job ID
     * @return average duration in milliseconds, or null if no successful executions
     */
    @Query("""
        SELECT AVG(e.durationMs) FROM JobExecution e
        WHERE e.job.id = :jobId
          AND e.status = 'SUCCESS'
          AND e.durationMs IS NOT NULL
        """)
    Double getAverageDuration(@Param("jobId") Long jobId);
    
    /**
     * Counts executions by status for a specific job.
     * 
     * @param jobId the job ID
     * @param status the execution status
     * @return count of executions
     */
    long countByJobIdAndStatus(Long jobId, ExecutionStatus status);
    
    /**
     * Finds the highest epoch number in the execution history.
     * 
     * Interview Talking Point:
     * "I can query the highest epoch to verify that the fencing token mechanism
     * is working correctly. The current leader's epoch should always be the highest."
     * 
     * @return the highest epoch number, or 0 if no executions
     */
    @Query("""
        SELECT MAX(
            CAST(
                SUBSTRING(e.fencingToken, 6, LOCATE('-node', e.fencingToken) - 6)
                AS long
            )
        )
        FROM JobExecution e
        WHERE e.fencingToken LIKE 'epoch%'
        """)
    Long findMaxEpoch();
}

