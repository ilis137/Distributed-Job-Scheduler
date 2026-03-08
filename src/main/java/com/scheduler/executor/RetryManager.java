package com.scheduler.executor;

import com.scheduler.config.SchedulerProperties;
import com.scheduler.domain.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Manages retry logic for failed job executions.
 * 
 * Implements exponential backoff with jitter to prevent thundering herd:
 * - Base delay doubles with each retry attempt
 * - Jitter (random variation) prevents all retries from happening simultaneously
 * - Maximum delay cap prevents excessive wait times
 * 
 * Interview Talking Points:
 * - "I use exponential backoff to give failing systems time to recover"
 * - "Jitter prevents thundering herd when multiple jobs fail simultaneously"
 * - "The formula is: delay = min(initialDelay * 2^retryCount, maxDelay) + jitter"
 * - "Jitter is random(0, delay * 0.1) to add 0-10% variation"
 * 
 * Design Decision:
 * "I chose exponential backoff over fixed delays because:
 * 1. Gives downstream systems time to recover from failures
 * 2. Reduces load on failing systems (circuit breaker pattern)
 * 3. Industry standard for retry logic (AWS, Google Cloud, etc.)
 * 4. Jitter prevents synchronized retries that could overwhelm the system"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetryManager {
    
    private final SchedulerProperties properties;
    private final Random random = new Random();
    
    /**
     * Calculates the next retry time for a failed job.
     * 
     * Uses exponential backoff with jitter:
     * delay = min(initialDelay * backoffMultiplier^retryCount, maxDelay) + jitter
     * 
     * Interview Talking Point:
     * "I calculate retry delay using exponential backoff. For example, with
     * initialDelay=30s and multiplier=2.0, the delays are: 30s, 60s, 120s, 240s.
     * I add jitter (0-10% random variation) to prevent thundering herd."
     * 
     * @param job the job that failed
     * @return the next retry time
     */
    public Instant calculateNextRetryTime(Job job) {
        int retryCount = job.getRetryCount();
        
        // Calculate base delay with exponential backoff
        long baseDelaySeconds = calculateBaseDelay(retryCount);
        
        // Add jitter (0-10% of base delay)
        long jitterSeconds = calculateJitter(baseDelaySeconds);
        
        long totalDelaySeconds = baseDelaySeconds + jitterSeconds;
        
        Instant nextRetryTime = Instant.now().plusSeconds(totalDelaySeconds);
        
        log.debug("Calculated next retry time for job {} (retry {}): {} (delay: {}s, jitter: {}s)",
            job.getName(), retryCount, nextRetryTime, baseDelaySeconds, jitterSeconds);
        
        return nextRetryTime;
    }
    
    /**
     * Calculates the base delay using exponential backoff.
     * 
     * Formula: min(initialDelay * multiplier^retryCount, maxDelay)
     * 
     * @param retryCount the current retry attempt number
     * @return the base delay in seconds
     */
    private long calculateBaseDelay(int retryCount) {
        long initialDelay = properties.getJobExecution().getInitialRetryDelaySeconds();
        double multiplier = properties.getJobExecution().getRetryBackoffMultiplier();
        long maxDelay = properties.getJobExecution().getMaxRetryDelaySeconds();
        
        // Calculate: initialDelay * multiplier^retryCount
        double delay = initialDelay * Math.pow(multiplier, retryCount);
        
        // Cap at maxDelay
        return Math.min((long) delay, maxDelay);
    }
    
    /**
     * Calculates jitter (random variation) to add to the delay.
     * 
     * Jitter is 0-10% of the base delay to prevent thundering herd.
     * 
     * @param baseDelaySeconds the base delay in seconds
     * @return the jitter in seconds
     */
    private long calculateJitter(long baseDelaySeconds) {
        // Jitter is 0-10% of base delay
        double maxJitter = baseDelaySeconds * 0.1;
        return (long) (random.nextDouble() * maxJitter);
    }
    
    /**
     * Checks if a job should be retried after a failure.
     * 
     * A job should be retried if:
     * 1. Retry count < max retries
     * 2. Job is enabled
     * 
     * @param job the job that failed
     * @return true if the job should be retried
     */
    public boolean shouldRetry(Job job) {
        boolean hasRetriesRemaining = job.shouldRetry();
        boolean isEnabled = job.getEnabled();
        
        boolean shouldRetry = hasRetriesRemaining && isEnabled;
        
        if (!shouldRetry) {
            if (!hasRetriesRemaining) {
                log.warn("Job {} has exhausted all retry attempts ({}/{})",
                    job.getName(), job.getRetryCount(), job.getMaxRetries());
            }
            if (!isEnabled) {
                log.warn("Job {} is disabled - will not retry", job.getName());
            }
        }
        
        return shouldRetry;
    }
    
    /**
     * Gets the retry delay for a specific retry attempt.
     * 
     * Useful for displaying retry schedule to users.
     * 
     * @param retryAttempt the retry attempt number (0-based)
     * @return the delay duration
     */
    public Duration getRetryDelay(int retryAttempt) {
        long baseDelaySeconds = calculateBaseDelay(retryAttempt);
        return Duration.ofSeconds(baseDelaySeconds);
    }
}

