# Interview-Grade Architecture - Implementation Plan

**Date Started**: 2026-03-07  
**Architecture**: Simplified ~40-class design focused on distributed systems  
**Timeline**: 5 weeks  
**Status**: 🚀 Week 1 - IN PROGRESS

---

## Implementation Strategy

Following the architecture review recommendations, we're implementing a **focused, interview-optimized** version that showcases distributed systems expertise.

**Key Principle**: 70% distributed systems, 30% supporting infrastructure

---

## Week 1: Domain + Database Layer ⏳ IN PROGRESS

**Goal**: Establish solid persistence foundation

### 1.1 Domain Enums ✅
**Files**:
- `domain/enums/JobStatus.java` - Job lifecycle states
- `domain/enums/ExecutionStatus.java` - Execution result states
- `domain/enums/NodeRole.java` - Scheduler node roles (LEADER, FOLLOWER)

**Key Features**:
- State transition validation methods
- Clear, self-documenting enum values

### 1.2 Domain Entities ⏳
**Files**:
- `domain/entity/Job.java` - Core job definition (JPA entity)
- `domain/entity/JobExecution.java` - Execution history with fencing tokens
- `domain/entity/SchedulerNode.java` - Cluster node tracking

**Key Features**:
- JPA annotations for persistence
- Optimistic locking with `@Version`
- Domain logic methods (state transitions, validation)
- Lombok for boilerplate reduction
- Audit fields (createdAt, updatedAt)

### 1.3 JPA Repositories ⏳
**Files**:
- `repository/JobRepository.java`
- `repository/JobExecutionRepository.java`
- `repository/SchedulerNodeRepository.java`

**Key Features**:
- Custom query methods for job polling
- Fencing token validation queries
- Leader node queries

### 1.4 Database Schema (Flyway) ✅
**Files**:
- `db/changelog/db.changelog-master.xml`
- `db/changelog/v1.0/001-create-jobs-table.xml`
- `db/changelog/v1.0/002-create-job-executions-table.xml`
- `db/changelog/v1.0/003-create-scheduler-nodes-table.xml`
- `db/changelog/v1.0/004-create-indexes.xml`

**Key Features**:
- Proper indexes for job polling (`next_run_time`, `status`)
- Fencing token column in executions table
- Foreign key constraints
- Audit columns (created_at, updated_at)

### 1.5 Configuration ⏳
**Files**:
- `config/DatabaseConfig.java` - HikariCP connection pooling

**Key Features**:
- Connection pool tuning
- Transaction management
- Leak detection

**Success Criteria**:
- ✅ Maven build passes
- ✅ Flyway migrations run successfully
- ✅ Can persist Job entity to database
- ✅ Can query jobs by status and next_run_time
- ✅ Integration test: CRUD operations work

---

## Week 2: Coordination Layer ⏸️ NEXT

**Goal**: Implement core distributed systems patterns (⭐ MOST IMPORTANT)

### 2.1 Coordination Abstraction
**Files**:
- `coordination/CoordinationService.java` (interface)
- `coordination/RedisCoordinationService.java` (Redisson implementation)

**Key Features**:
- Abstraction for testability
- Leader election primitives
- Distributed lock primitives

### 2.2 Leader Election
**Files**:
- `coordination/LeaderElectionService.java`
- `coordination/HeartbeatService.java`

**Key Features**:
- TTL-based leases (10s TTL, 3s heartbeat)
- Automatic failover
- Jittered backoff for followers

### 2.3 Distributed Locking
**Files**:
- `coordination/DistributedLockService.java`
- `coordination/FencingTokenProvider.java`

**Key Features**:
- Redlock algorithm via Redisson
- Fencing tokens with epoch numbers
- Lock renewal for long-running jobs

**Success Criteria**:
- ✅ Single leader elected from 3 nodes
- ✅ Automatic failover when leader dies
- ✅ Fencing tokens prevent split-brain
- ✅ Chaos test: Kill leader, verify new leader elected

---

## Week 3: Execution Layer ⏸️

**Goal**: Implement job execution with virtual threads

### 3.1 Job Executor
**Files**:
- `executor/JobExecutor.java`
- `executor/VirtualThreadExecutor.java`
- `executor/RetryManager.java`

**Key Features**:
- Virtual thread pool (Java 21)
- Retry with exponential backoff
- Execution context tracking

### 3.2 Service Layer
**Files**:
- `service/JobService.java`
- `service/JobExecutionService.java`

**Key Features**:
- Job CRUD operations
- Job scheduling logic
- Execution orchestration

**Success Criteria**:
- ✅ Leader polls for due jobs
- ✅ Jobs execute in virtual threads
- ✅ Failed jobs retry with backoff
- ✅ Execution history recorded with fencing tokens

---

## Week 4: API Layer ⏸️

**Goal**: REST API with simple DTOs

### 4.1 DTOs (Java Records)
**Files**:
- `api/dto/CreateJobRequest.java` (record)
- `api/dto/JobResponse.java` (record)
- `api/dto/ErrorResponse.java` (record)

**Key Features**:
- Java records for immutability
- Simple `toEntity()` methods (no MapStruct)
- JSR-380 validation annotations

### 4.2 Controllers
**Files**:
- `api/controller/JobController.java`
- `api/controller/ClusterController.java`

**Key Features**:
- Basic CRUD endpoints
- Cluster status endpoint
- OpenAPI/Swagger annotations

### 4.3 Exception Handling
**Files**:
- `common/exception/SchedulerException.java`
- `common/exception/GlobalExceptionHandler.java`

**Success Criteria**:
- ✅ Can submit jobs via REST API
- ✅ Can query job status
- ✅ Can view cluster status
- ✅ Proper error responses

---

## Week 5: Testing + Documentation ⏸️

**Goal**: Comprehensive testing and documentation

### 5.1 Integration Tests
- Leader election flow
- Distributed locking
- Job execution end-to-end

### 5.2 Chaos Tests
- Kill leader during execution
- Network partition simulation
- Redis failure handling

### 5.3 Documentation
- README with architecture overview
- API documentation
- Deployment guide

**Success Criteria**:
- ✅ 70%+ test coverage
- ✅ All chaos tests pass
- ✅ Can demo to interviewers

---

## Package Structure (Final)

```
com.scheduler/
├── SchedulerApplication.java
│
├── api/
│   ├── controller/
│   │   ├── JobController.java
│   │   └── ClusterController.java
│   └── dto/
│       ├── CreateJobRequest.java (record)
│       ├── JobResponse.java (record)
│       └── ErrorResponse.java (record)
│
├── service/
│   ├── JobService.java
│   └── JobExecutionService.java
│
├── domain/
│   ├── entity/
│   │   ├── Job.java
│   │   ├── JobExecution.java
│   │   └── SchedulerNode.java
│   ├── enums/
│   │   ├── JobStatus.java
│   │   ├── ExecutionStatus.java
│   │   └── NodeRole.java
│   └── event/
│       └── JobExecutedEvent.java
│
├── coordination/          # ⭐ STAR OF THE SHOW
│   ├── CoordinationService.java
│   ├── RedisCoordinationService.java
│   ├── LeaderElectionService.java
│   ├── DistributedLockService.java
│   ├── FencingTokenProvider.java
│   └── HeartbeatService.java
│
├── executor/
│   ├── JobExecutor.java
│   ├── VirtualThreadExecutor.java
│   └── RetryManager.java
│
├── repository/
│   ├── JobRepository.java
│   ├── JobExecutionRepository.java
│   └── SchedulerNodeRepository.java
│
├── config/
│   ├── DatabaseConfig.java
│   ├── RedisConfig.java
│   └── ExecutorConfig.java
│
└── common/
    ├── exception/
    │   ├── SchedulerException.java
    │   ├── JobNotFoundException.java
    │   └── GlobalExceptionHandler.java
    └── util/
        └── CorrelationIdUtil.java
```

**Total Classes**: ~40 (vs. 80+ in production-grade plan)

---

## Current Progress

- [/] Week 1: Domain + Database (IN PROGRESS)
- [ ] Week 2: Coordination (NEXT)
- [ ] Week 3: Execution
- [ ] Week 4: API
- [ ] Week 5: Testing + Documentation

---

## Next Immediate Steps

1. Create domain enums (JobStatus, ExecutionStatus, NodeRole)
2. Create domain entities (Job, JobExecution, SchedulerNode)
3. Create JPA repositories
4. Create Flyway migrations
5. Write integration test to verify persistence

**Let's start!** 🚀

