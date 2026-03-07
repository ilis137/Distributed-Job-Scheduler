package com.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the Distributed Job Scheduler.
 * <p>
 * This application provides a highly available, fault-tolerant job scheduling
 * system with the following key features:
 * <ul>
 *   <li>Leader election using Redis for coordination</li>
 *   <li>Distributed locking with Redlock algorithm</li>
 *   <li>Automatic failover when leader node crashes</li>
 *   <li>Retry logic with exponential backoff</li>
 *   <li>Persistent job storage in MySQL</li>
 *   <li>Comprehensive observability with Prometheus metrics</li>
 * </ul>
 * </p>
 *
 * <p>
 * The system demonstrates advanced distributed systems concepts including:
 * <ul>
 *   <li>CAP theorem trade-offs (AP system with Redis)</li>
 *   <li>Fencing tokens for split-brain prevention</li>
 *   <li>Idempotency keys for exactly-once semantics</li>
 *   <li>Job state machine with proper transitions</li>
 *   <li>Dead letter queue for failed jobs</li>
 * </ul>
 * </p>
 *
 * @author Scheduler Team
 * @version 1.0.0
 * @since 2026-03-07
 */
@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

    /**
     * Main entry point for the Distributed Job Scheduler application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}

