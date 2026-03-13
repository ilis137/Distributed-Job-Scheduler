package com.scheduler.repository;

import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
 * - Uses @EntityGraph to eagerly fetch Job association when needed for API responses
 *
 * Design Decision:
 * "I use @EntityGraph to solve the LazyInitializationException when fetching executions
 * for API responses. This eagerly fetches the Job association in a single JOIN query,
 * avoiding N+1 queries while maintaining lazy loading as the default for other use cases.
 * This is cleaner than using @Transactional on the controller or JOIN FETCH in every query."
 */
@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {

    /**
     * Finds an execution by ID with the Job association eagerly fetched.
     *
     * This method is used by API endpoints that need to return job details
     * in the execution response. The @EntityGraph annotation tells Hibernate
     * to fetch the Job association in the same query, preventing LazyInitializationException.
     *
     * Interview Talking Point:
     * "I use @EntityGraph to eagerly fetch the Job association when needed for API responses.
     * This solves the LazyInitializationException without requiring open-in-view=true or
     * @Transactional on the controller. The default findById() still uses lazy loading for
     * other use cases where the job details aren't needed."
     *
     * @param id the execution ID
     * @return Optional containing the execution with job eagerly loaded
     */
    @EntityGraph(attributePaths = {"job"})
    Optional<JobExecution> findWithJobById(Long id);

    /**
     * Finds all executions (paginated) with Job eagerly fetched.
     *
     * Overrides the default findAll to eagerly fetch the Job association
     * for API responses that list all executions.
     *
     * @param pageable pagination parameters
     * @return page of executions with job association loaded
     */
    @EntityGraph(attributePaths = {"job"})
    Page<JobExecution> findAll(Pageable pageable);
    
    /**
     * Finds all executions for a specific job, ordered by creation time.
     *
     * @param jobId the job ID
     * @return list of executions for the job
     */
    @EntityGraph(attributePaths = {"job"})
    @Query("SELECT e FROM JobExecution e WHERE e.job.id = :jobId ORDER BY e.createdAt DESC")
    List<JobExecution> findByJobId(@Param("jobId") Long jobId);

    /**
     * Finds all executions for a specific job (paginated) with Job eagerly fetched.
     *
     * Used by API endpoints that return execution history with job details.
     *
     * @param jobId the job ID
     * @param pageable pagination parameters
     * @return page of executions for the job with job association loaded
     */
    @EntityGraph(attributePaths = {"job"})
    @Query("SELECT e FROM JobExecution e WHERE e.job.id = :jobId ORDER BY e.createdAt DESC")
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
    @EntityGraph(attributePaths = {"job"})
    List<JobExecution> findByStatus(ExecutionStatus status);

    /**
     * Finds executions with a specific status (paginated) with Job eagerly fetched.
     *
     * Used by API endpoints that filter executions by status.
     *
     * @param status the execution status
     * @param pageable pagination parameters
     * @return page of executions with the status and job association loaded
     */
    @EntityGraph(attributePaths = {"job"})
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

