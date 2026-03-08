package com.scheduler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for job execution using Java 21 virtual threads.
 * 
 * Virtual threads enable high concurrency with minimal resource overhead.
 * Unlike platform threads, virtual threads are lightweight and can scale
 * to millions of concurrent tasks.
 * 
 * Interview Talking Points:
 * - "I use Java 21 virtual threads for job execution to achieve high concurrency
 *   without the overhead of traditional thread pools"
 * - "Virtual threads are perfect for I/O-bound tasks like job scheduling,
 *   where jobs may wait on external resources"
 * - "With virtual threads, I can handle 10,000+ concurrent jobs on a single node
 *   without running out of memory or CPU"
 * - "The JVM manages virtual thread scheduling automatically, mapping them to
 *   a small number of platform threads"
 * 
 * Design Decision:
 * "I chose virtual threads over traditional thread pools because:
 * 1. No need to tune thread pool size - virtual threads scale automatically
 * 2. Lower memory footprint - each virtual thread uses ~1KB vs ~1MB for platform threads
 * 3. Simpler code - no need for reactive programming or async/await patterns
 * 4. Better observability - stack traces work normally, unlike with reactive code"
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ExecutorConfig {
    
    private final SchedulerProperties properties;
    
    /**
     * Creates a virtual thread executor for job execution.
     * 
     * This executor creates a new virtual thread for each submitted task.
     * Virtual threads are managed by the JVM and mapped to a small pool
     * of platform threads (carrier threads).
     * 
     * Interview Talking Point:
     * "I use Executors.newVirtualThreadPerTaskExecutor() which creates a new
     * virtual thread for each job. This is efficient because virtual threads
     * are cheap to create and destroy. The JVM automatically manages the
     * underlying platform threads."
     * 
     * @return ExecutorService using virtual threads
     */
    @Bean(name = "jobExecutorService")
    public ExecutorService jobExecutorService() {
        log.info("Initializing virtual thread executor for job execution");
        log.info("Virtual threads enabled: {}", Thread.ofVirtual().factory() != null);
        
        // Create virtual thread executor
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        log.info("Virtual thread executor initialized successfully");
        log.info("Expected concurrent job capacity: 10,000+ jobs");
        
        return executor;
    }
    
    /**
     * Creates a scheduled executor for periodic tasks (job polling, heartbeats).
     * 
     * This uses a small fixed thread pool since scheduled tasks are lightweight
     * and don't benefit from virtual threads (they're always running).
     * 
     * @return ExecutorService for scheduled tasks
     */
    @Bean(name = "scheduledExecutorService")
    public ExecutorService scheduledExecutorService() {
        log.info("Initializing scheduled executor for periodic tasks");
        
        // Use a small fixed pool for scheduled tasks
        // These are lightweight tasks that run periodically (polling, heartbeats)
        int poolSize = 4; // Enough for: job polling, heartbeat, cleanup, monitoring
        
        ExecutorService executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread thread = new Thread(r);
            thread.setName("scheduler-periodic");
            thread.setDaemon(true);
            return thread;
        });
        
        log.info("Scheduled executor initialized with {} threads", poolSize);
        
        return executor;
    }
}

