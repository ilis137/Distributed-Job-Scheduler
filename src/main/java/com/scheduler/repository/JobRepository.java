package com.scheduler.repository;

import com.scheduler.domain.entity.Job;
import com.scheduler.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Job entity.
 * 
 * Interview Talking Points:
 * - Custom query methods for efficient job polling
 * - Uses indexed columns for performance
 * - Supports distributed job scheduling patterns
 * 
 * Design Decision:
 * "I use Spring Data JPA for repository abstraction. The findDueJobs query is
 * critical for performance - it uses the composite index on (status, next_run_time)
 * to efficiently find jobs that need execution. The LIMIT clause prevents the
 * leader from being overwhelmed if there's a large backlog."
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    
    /**
     * Finds a job by its unique name.
     * 
     * @param name the job name
     * @return Optional containing the job if found
     */
    Optional<Job> findByName(String name);
    
    /**
     * Finds all jobs with the given status.
     * 
     * @param status the job status
     * @return list of jobs with the given status
     */
    List<Job> findByStatus(JobStatus status);
    
    /**
     * Finds jobs that are due for execution.
     * 
     * This is the CRITICAL query for the scheduler. The leader polls this
     * query every second to find jobs that need execution.
     * 
     * Interview Talking Point:
     * "This query is optimized with a composite index on (status, next_run_time).
     * I limit the results to prevent overwhelming the system if there's a large
     * backlog. The leader processes jobs in batches, acquiring distributed locks
     * for each one."
     * 
     * @param now the current time
     * @param limit maximum number of jobs to return
     * @return list of jobs due for execution
     */
    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'PENDING'
          AND j.enabled = true
          AND j.nextRunTime <= :now
        ORDER BY j.nextRunTime ASC
        LIMIT :limit
        """)
    List<Job> findDueJobs(@Param("now") Instant now, @Param("limit") int limit);
    
    /**
     * Finds all enabled recurring jobs.
     * 
     * @return list of recurring jobs
     */
    @Query("SELECT j FROM Job j WHERE j.recurring = true AND j.enabled = true")
    List<Job> findRecurringJobs();
    
    /**
     * Finds jobs that are stuck in RUNNING status for longer than the timeout.
     * These jobs may have failed without updating their status (e.g., node crash).
     * 
     * Interview Talking Point:
     * "I have a background task that periodically checks for stuck jobs.
     * If a job has been RUNNING for longer than its timeout, it's likely the
     * node crashed. I mark it as FAILED so it can be retried."
     * 
     * @param threshold the time threshold (jobs running longer than this are stuck)
     * @return list of stuck jobs
     */
    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'RUNNING'
          AND j.updatedAt < :threshold
        """)
    List<Job> findStuckJobs(@Param("threshold") Instant threshold);
    
    /**
     * Counts jobs by status.
     * Useful for monitoring and dashboards.
     * 
     * @param status the job status
     * @return count of jobs with the given status
     */
    long countByStatus(JobStatus status);
    
    /**
     * Finds jobs in DEAD_LETTER status (requires manual intervention).
     * 
     * @return list of jobs in dead letter queue
     */
    @Query("SELECT j FROM Job j WHERE j.status = 'DEAD_LETTER' ORDER BY j.updatedAt DESC")
    List<Job> findDeadLetterJobs();
}

