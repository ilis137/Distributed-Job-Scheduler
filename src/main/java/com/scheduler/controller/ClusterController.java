package com.scheduler.controller;

import com.scheduler.coordination.LeaderElectionService;
import com.scheduler.domain.entity.SchedulerNode;
import com.scheduler.domain.enums.JobStatus;
import com.scheduler.dto.ClusterStatusResponse;
import com.scheduler.dto.DtoMapper;
import com.scheduler.dto.NodeStatusResponse;
import com.scheduler.repository.JobRepository;
import com.scheduler.repository.SchedulerNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for cluster status and monitoring.
 * 
 * Provides read-only access to cluster state for observability.
 * 
 * Interview Talking Points:
 * - "Cluster status endpoint provides observability into distributed system health"
 * - "Shows which node is leader and which are followers"
 * - "Helps diagnose leader election issues and network partitions"
 * - "Read-only endpoints can be called from any node (no leader requirement)"
 * 
 * Design Decision:
 * "Cluster endpoints are read-only because cluster state is managed by the
 * coordination layer (leader election, heartbeats). Manual intervention would
 * bypass the distributed consensus mechanism and could cause split-brain."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/cluster")
@RequiredArgsConstructor
@Slf4j
public class ClusterController {
    
    private final SchedulerNodeRepository nodeRepository;
    private final JobRepository jobRepository;
    private final LeaderElectionService leaderElectionService;
    
    /**
     * Gets overall cluster status.
     * 
     * Interview Talking Point:
     * "Cluster status aggregates information from multiple sources:
     * - Node health from heartbeat service
     * - Job counts from database
     * - Leader information from coordination service
     * This provides a single endpoint for monitoring dashboards."
     * 
     * @return 200 OK with cluster status
     */
    @GetMapping("/status")
    public ResponseEntity<ClusterStatusResponse> getClusterStatus() {
        log.debug("Fetching cluster status");

        List<SchedulerNode> nodes = nodeRepository.findAll();

        // Find leader node ID from nodes with LEADER role
        String leaderNodeId = nodes.stream()
            .filter(node -> node.getRole() == com.scheduler.domain.enums.NodeRole.LEADER)
            .map(SchedulerNode::getNodeId)
            .findFirst()
            .orElse(null);

        List<NodeStatusResponse> nodeStatuses = nodes.stream()
            .map(DtoMapper::toNodeStatusResponse)
            .collect(Collectors.toList());

        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.countByStatus(JobStatus.RUNNING);
        long pendingJobs = jobRepository.countByStatus(JobStatus.PENDING);
        long failedJobs = jobRepository.countByStatus(JobStatus.FAILED);

        int totalNodes = nodes.size();
        int healthyNodes = (int) nodes.stream().filter(node -> node.getHealthy()).count();
        
        ClusterStatusResponse response = new ClusterStatusResponse(
            nodeStatuses,
            leaderNodeId,
            totalNodes,
            healthyNodes,
            totalJobs,
            activeJobs,
            pendingJobs,
            failedJobs
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lists all scheduler nodes in the cluster.
     * 
     * @return 200 OK with list of nodes
     */
    @GetMapping("/nodes")
    public ResponseEntity<List<NodeStatusResponse>> listNodes() {
        log.debug("Listing all cluster nodes");
        
        List<SchedulerNode> nodes = nodeRepository.findAll();
        List<NodeStatusResponse> response = nodes.stream()
            .map(DtoMapper::toNodeStatusResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets current leader information.
     * 
     * Interview Talking Point:
     * "The leader endpoint shows which node is currently executing jobs.
     * During failover, this will change to a different node. The epoch
     * number increments on each leader election, which is used for fencing."
     * 
     * @return 200 OK with leader node details, or 404 if no leader
     */
    @GetMapping("/leader")
    public ResponseEntity<NodeStatusResponse> getLeader() {
        log.debug("Fetching current leader");

        // Find leader node from database
        SchedulerNode leader = nodeRepository.findAll().stream()
            .filter(node -> node.getRole() == com.scheduler.domain.enums.NodeRole.LEADER)
            .findFirst()
            .orElse(null);

        if (leader == null) {
            log.warn("No leader currently elected");
            return ResponseEntity.notFound().build();
        }

        NodeStatusResponse response = DtoMapper.toNodeStatusResponse(leader);

        return ResponseEntity.ok(response);
    }
}

