package com.scheduler.dto;

import java.util.List;

/**
 * Response DTO for cluster status information.
 * 
 * Interview Talking Points:
 * - "Cluster status provides observability into distributed system health"
 * - "Shows which node is leader and which are followers"
 * - "Helps diagnose leader election issues and network partitions"
 * 
 * @param nodes List of all scheduler nodes in the cluster
 * @param leaderNodeId ID of the current leader node
 * @param totalNodes Total number of nodes in cluster
 * @param healthyNodes Number of healthy nodes
 * @param totalJobs Total number of jobs in system
 * @param activeJobs Number of currently executing jobs
 * @param pendingJobs Number of jobs waiting to execute
 * @param failedJobs Number of jobs in failed state
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public record ClusterStatusResponse(
    List<NodeStatusResponse> nodes,
    String leaderNodeId,
    int totalNodes,
    int healthyNodes,
    long totalJobs,
    long activeJobs,
    long pendingJobs,
    long failedJobs
) {
}

