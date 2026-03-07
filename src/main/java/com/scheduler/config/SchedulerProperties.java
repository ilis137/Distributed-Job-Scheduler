package com.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for the Distributed Job Scheduler.
 * 
 * Binds to the 'scheduler' prefix in application.yml.
 * 
 * Interview Talking Point:
 * "I use @ConfigurationProperties for type-safe configuration binding.
 * This provides validation, IDE autocomplete, and makes it easy to override
 * settings per environment (dev, prod, test)."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "scheduler")
@Data
@Validated
public class SchedulerProperties {
    
    /**
     * Node configuration.
     */
    private Node node = new Node();
    
    /**
     * Leader election configuration.
     */
    private LeaderElection leaderElection = new LeaderElection();
    
    /**
     * Job execution configuration.
     */
    private JobExecution jobExecution = new JobExecution();
    
    /**
     * Distributed lock configuration.
     */
    private DistributedLock distributedLock = new DistributedLock();
    
    @Data
    public static class Node {
        /**
         * Unique identifier for this scheduler node.
         * Defaults to hostname-port.
         */
        @NotBlank
        private String id = "localhost-8080";
    }
    
    @Data
    public static class LeaderElection {
        /**
         * Whether leader election is enabled.
         */
        private boolean enabled = true;
        
        /**
         * Redis key for the leader lock.
         */
        @NotBlank
        private String lockKey = "scheduler:leader";
        
        /**
         * TTL for the leader lock in seconds.
         * Leader must renew before this expires.
         */
        @Min(1)
        private int lockTtlSeconds = 10;
        
        /**
         * Interval between heartbeats in seconds.
         * Should be lockTtlSeconds / 3 for safety.
         */
        @Min(1)
        private int heartbeatIntervalSeconds = 3;
    }
    
    @Data
    public static class JobExecution {
        /**
         * Size of the virtual thread pool for job execution.
         */
        @Min(1)
        private int threadPoolSize = 50;
        
        /**
         * Maximum number of retry attempts for failed jobs.
         */
        @Min(0)
        private int maxRetryAttempts = 3;
        
        /**
         * Initial retry delay in seconds.
         */
        @Min(1)
        private int initialRetryDelaySeconds = 30;
        
        /**
         * Maximum retry delay in seconds.
         */
        @Min(1)
        private int maxRetryDelaySeconds = 300;
        
        /**
         * Backoff multiplier for exponential backoff.
         */
        @Min(1)
        private double retryBackoffMultiplier = 2.0;
    }
    
    @Data
    public static class DistributedLock {
        /**
         * Default TTL for distributed locks in seconds.
         */
        @Min(1)
        private int lockTtlSeconds = 60;
        
        /**
         * Maximum time to wait for lock acquisition in seconds.
         */
        @Min(0)
        private int waitTimeSeconds = 5;
        
        /**
         * Lease time for locks in seconds.
         * Lock is automatically released after this time.
         */
        @Min(1)
        private int leaseTimeSeconds = 60;
    }
}

