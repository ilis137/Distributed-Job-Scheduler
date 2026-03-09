package com.scheduler.dto;

import com.scheduler.domain.entity.Job;
import com.scheduler.domain.entity.JobExecution;
import com.scheduler.domain.entity.SchedulerNode;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between domain entities and DTOs.
 * 
 * Interview Talking Points:
 * - "Mapper centralizes conversion logic and keeps controllers clean"
 * - "Static methods are sufficient for simple mappings (no need for MapStruct)"
 * - "Could use MapStruct for complex mappings with nested objects"
 * 
 * Design Decision:
 * "I use a simple mapper class instead of MapStruct because our mappings are
 * straightforward. If mappings become complex (nested objects, custom logic),
 * I would switch to MapStruct for compile-time safety and performance."
 * 
 * @author Distributed Job Scheduler Team
 * @since 1.0.0
 */
public class DtoMapper {
    
    private DtoMapper() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Converts Job entity to JobResponse DTO.
     */
    public static JobResponse toJobResponse(Job job) {
        return new JobResponse(
            job.getId(),
            job.getName(),
            job.getDescription(),
            job.getStatus(),
            job.getCronExpression(),
            job.getNextRunTime(),
            job.getRetryCount(),
            job.getMaxRetries(),
            job.getTimeoutSeconds(),
            job.getEnabled(),
            job.getCreatedAt(),
            job.getUpdatedAt()
        );
    }
    
    /**
     * Converts Page of Jobs to JobListResponse.
     */
    public static JobListResponse toJobListResponse(Page<Job> page) {
        List<JobResponse> jobs = page.getContent()
            .stream()
            .map(DtoMapper::toJobResponse)
            .collect(Collectors.toList());
        
        return new JobListResponse(
            jobs,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
    
    /**
     * Converts JobExecution entity to JobExecutionResponse DTO.
     */
    public static JobExecutionResponse toJobExecutionResponse(JobExecution execution) {
        Long durationMs = JobExecutionResponse.calculateDuration(
            execution.getStartedAt(),
            execution.getCompletedAt()
        );

        return new JobExecutionResponse(
            execution.getId(),
            execution.getJob().getId(),
            execution.getJob().getName(),
            execution.getStatus(),
            execution.getNodeId(),
            execution.getStartedAt(),
            execution.getCompletedAt(),
            durationMs,
            execution.getErrorMessage(),
            execution.getErrorStackTrace(),
            execution.getFencingToken(),
            execution.getRetryAttempt()
        );
    }
    
    /**
     * Converts Page of JobExecutions to ExecutionHistoryResponse.
     */
    public static ExecutionHistoryResponse toExecutionHistoryResponse(Page<JobExecution> page) {
        List<JobExecutionResponse> executions = page.getContent()
            .stream()
            .map(DtoMapper::toJobExecutionResponse)
            .collect(Collectors.toList());
        
        return new ExecutionHistoryResponse(
            executions,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
    
    /**
     * Converts SchedulerNode entity to NodeStatusResponse DTO.
     */
    public static NodeStatusResponse toNodeStatusResponse(SchedulerNode node) {
        return new NodeStatusResponse(
            node.getNodeId(),
            node.getRole(),
            node.getHealthy(),
            node.getEpoch(),
            node.getLastHeartbeatAt(),
            node.getCreatedAt(),
            node.getVersion() != null ? node.getVersion() : "1.0.0"
        );
    }
}

