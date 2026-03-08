package com.scheduler.executor;

import com.scheduler.coordination.LeaderElectionService;
import com.scheduler.domain.entity.Job;
import com.scheduler.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled task that polls for due jobs and submits them for execution.
 * 
 * This is the heart of the job scheduler - it runs every second on the leader
 * node and finds jobs that are due for execution.
 * 
 * Interview Talking Points:
 * - "Only the leader polls for jobs - followers are on standby for failover"
 * - "I poll every 1 second to ensure jobs execute close to their scheduled time"
 * - "I limit the batch size to prevent overwhelming the system during backlog"
 * - "Jobs are submitted to virtual thread executor for concurrent execution"
 * - "Distributed locks prevent duplicate execution if multiple nodes poll simultaneously"
 * 
 * Design Decision:
 * "I use @Scheduled with fixedDelay instead of fixedRate because:
 * 1. fixedDelay waits for previous poll to complete before starting next
 * 2. Prevents overlapping polls if database query is slow
 * 3. More predictable behavior under load"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {
    
    private final JobService jobService;
    private final JobExecutor jobExecutor;
    private final LeaderElectionService leaderElectionService;
    
    private final AtomicInteger pollCount = new AtomicInteger(0);
    private final AtomicInteger jobsSubmitted = new AtomicInteger(0);
    
    /**
     * Polls for due jobs and submits them for execution.
     * 
     * Runs every 1 second with fixed delay.
     * Only executes if this node is the leader.
     * 
     * Interview Talking Point:
     * "I use @Scheduled with fixedDelay=1000ms to poll for jobs every second.
     * The leader check ensures only one node polls at a time. If the leader
     * fails, the new leader automatically takes over polling within seconds."
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void pollAndExecuteJobs() {
        // Only leader should poll for jobs
        if (!leaderElectionService.isLeader()) {
            return;
        }
        
        try {
            int currentPoll = pollCount.incrementAndGet();
            
            // Log every 60 seconds (every 60 polls)
            if (currentPoll % 60 == 0) {
                log.debug("Job polling active - poll #{}, jobs submitted: {}",
                    currentPoll, jobsSubmitted.get());
            }
            
            // Find due jobs (limit to 100 per poll to prevent overwhelming system)
            List<Job> dueJobs = jobService.findDueJobs(100);
            
            if (!dueJobs.isEmpty()) {
                log.info("Found {} due jobs - submitting for execution", dueJobs.size());
                
                // Submit each job for execution
                for (Job job : dueJobs) {
                    submitJob(job);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during job polling", e);
        }
    }
    
    /**
     * Submits a job for execution.
     * 
     * Interview Talking Point:
     * "I submit jobs asynchronously using CompletableFuture. This allows the
     * scheduler to continue polling while jobs execute in parallel. Virtual
     * threads make this efficient - I can have thousands of jobs executing
     * concurrently without running out of resources."
     * 
     * @param job the job to submit
     */
    private void submitJob(Job job) {
        try {
            log.info("Submitting job for execution: {} (ID: {})", job.getName(), job.getId());
            
            // Mark job as scheduled
            jobService.scheduleJob(job.getId());
            
            // Submit for async execution
            jobExecutor.executeAsync(job)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Job {} execution failed", job.getName(), error);
                    } else {
                        log.debug("Job {} execution completed", job.getName());
                    }
                });
            
            jobsSubmitted.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error submitting job: {}", job.getName(), e);
        }
    }
    
    /**
     * Gets the total number of polls performed.
     * 
     * @return the poll count
     */
    public int getPollCount() {
        return pollCount.get();
    }
    
    /**
     * Gets the total number of jobs submitted.
     * 
     * @return the jobs submitted count
     */
    public int getJobsSubmitted() {
        return jobsSubmitted.get();
    }
    
    /**
     * Resets the counters.
     * 
     * Used for testing.
     */
    public void resetCounters() {
        pollCount.set(0);
        jobsSubmitted.set(0);
    }
}

