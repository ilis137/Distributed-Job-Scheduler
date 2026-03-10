# Distributed Job Scheduler - Development Progress Tracker

**Project Start Date**: 2026-03-07
**Last Updated**: 2026-03-09 (Custom Bean Validation Implementation)
**Current Phase**: Phase 1 - Core Infrastructure
**Status**: ✅ COMPLETE

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Development Phases](#development-phases)
3. [Phase Progress Tracking](#phase-progress-tracking)
4. [Feature Documentation Index](#feature-documentation-index)
5. [Technology Stack](#technology-stack)
6. [Project Structure](#project-structure)
7. [Getting Started](#getting-started)

---

## Project Overview

A highly available, fault-tolerant distributed job scheduling system demonstrating advanced distributed systems concepts including leader election, distributed locking, automatic failover, and comprehensive observability.

**Key Objectives:**
- Showcase distributed systems expertise for technical interviews
- Implement production-ready patterns and practices
- Demonstrate deep understanding of CAP theorem, consistency models, and failure handling
- Build a portfolio project for senior/staff engineer positions

**Architecture Documentation**: See [ARCHITECTURE.md](./ARCHITECTURE.md) for comprehensive system design

---

## Development Phases

### Phase 1: Core Infrastructure ⏳ IN PROGRESS
**Goal**: Establish project foundation with database, basic entities, and single-node job execution

**Timeline**: Week 1-2
**Started**: 2026-03-07
**Completed**: _Not yet completed_

### Phase 2: Leader Election & Failover ⏸️ TODO
**Goal**: Implement Redis-based leader election with automatic failover

**Timeline**: Week 3
**Started**: _Not started_
**Completed**: _Not started_

### Phase 3: Distributed Locking & Job Execution ⏸️ TODO
**Goal**: Add distributed locks, retry logic, and job state management

**Timeline**: Week 4-5
**Started**: _Not started_
**Completed**: _Not started_

### Phase 4: Observability & Monitoring ⏸️ DEFERRED
**Goal**: Implement metrics, tracing, and health checks

**Timeline**: Week 6 (Deferred - will implement after Phase 3)
**Started**: _Not started_
**Completed**: _Not started_
**Note**: Observability features deferred to focus on core distributed systems functionality. See [docs/OBSERVABILITY_STRATEGY.md](./docs/OBSERVABILITY_STRATEGY.md)

### Phase 5: Advanced Features ⏸️ TODO
**Goal**: Add job dependencies, rate limiting, and graceful shutdown

**Timeline**: Week 7-8
**Started**: _Not started_
**Completed**: _Not started_

### Phase 6: Frontend & Documentation ⏸️ TODO
**Goal**: Build Angular UI and comprehensive documentation

**Timeline**: Week 9-10
**Started**: _Not started_
**Completed**: _Not started_

---

## Phase Progress Tracking

### Phase 1: Core Infrastructure

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Project Structure & Build Setup | ✅ COMPLETE | [docs/features/PROJECT_SETUP.md](./docs/features/PROJECT_SETUP.md) | Root | 2026-03-07 |
| Database Schema Design | ✅ COMPLETE | [docs/FLYWAY_MIGRATION_SUMMARY.md](./docs/FLYWAY_MIGRATION_SUMMARY.md) | `src/main/resources/db/migration/` | 2026-03-07 |
| Flyway Migration & Schema Validation Fix | ✅ COMPLETE | [docs/HIBERNATE_ENUM_VALIDATION_FIX.md](./docs/HIBERNATE_ENUM_VALIDATION_FIX.md) | `src/main/resources/` | 2026-03-07 |
| Core Domain Entities | ✅ COMPLETE | [docs/WEEK1_SUMMARY.md](./docs/WEEK1_SUMMARY.md) | `src/main/java/com/scheduler/domain/` | 2026-03-07 |
| JPA Repositories | ✅ COMPLETE | [docs/WEEK1_SUMMARY.md](./docs/WEEK1_SUMMARY.md) | `src/main/java/com/scheduler/repository/` | 2026-03-07 |
| Basic Configuration | ✅ COMPLETE | [docs/YAML_DUPLICATE_KEY_FIX.md](./docs/YAML_DUPLICATE_KEY_FIX.md) | `src/main/resources/` | 2026-03-07 |
| Coordination Layer (Week 2) | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Execution Layer (Week 3) | ✅ COMPLETE | [docs/WEEK3_EXECUTION_LAYER.md](./docs/WEEK3_EXECUTION_LAYER.md) | `src/main/java/com/scheduler/executor/`, `src/main/java/com/scheduler/service/` | 2026-03-08 |
| REST API Layer (Week 4) | ✅ COMPLETE | [docs/WEEK4_REST_API_LAYER.md](./docs/WEEK4_REST_API_LAYER.md) | `src/main/java/com/scheduler/controller/`, `src/main/java/com/scheduler/dto/` | 2026-03-09 |

### Phase 2: Leader Election & Failover

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Redis Configuration | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/config/` | 2026-03-07 |
| Coordination Service Abstraction | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Redis Coordination Implementation | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Leader Election Service | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Distributed Lock Service | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Fencing Token Provider | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |
| Heartbeat Service | ✅ COMPLETE | [docs/WEEK2_COORDINATION_LAYER.md](./docs/WEEK2_COORDINATION_LAYER.md) | `src/main/java/com/scheduler/coordination/` | 2026-03-07 |

### Phase 3: Distributed Locking & Job Execution

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Distributed Lock Service (Redlock) | ⏸️ TODO | [docs/features/DISTRIBUTED_LOCKS.md](./docs/features/DISTRIBUTED_LOCKS.md) | `src/main/java/com/scheduler/locking/` | - |
| Idempotency Service | ⏸️ TODO | [docs/features/IDEMPOTENCY.md](./docs/features/IDEMPOTENCY.md) | `src/main/java/com/scheduler/idempotency/` | - |
| Retry Manager | ⏸️ TODO | [docs/features/RETRY_LOGIC.md](./docs/features/RETRY_LOGIC.md) | `src/main/java/com/scheduler/retry/` | - |
| Job State Machine | ⏸️ TODO | [docs/features/STATE_MACHINE.md](./docs/features/STATE_MACHINE.md) | `src/main/java/com/scheduler/statemachine/` | - |
| Multi-Node Job Execution | ⏸️ TODO | [docs/features/DISTRIBUTED_EXECUTION.md](./docs/features/DISTRIBUTED_EXECUTION.md) | `src/main/java/com/scheduler/executor/` | - |

### Phase 4: Observability & Monitoring

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Prometheus Metrics | ⏸️ TODO | [docs/features/METRICS.md](./docs/features/METRICS.md) | `src/main/java/com/scheduler/metrics/` | - |
| Custom Health Indicators | ⏸️ TODO | [docs/features/HEALTH_CHECKS.md](./docs/features/HEALTH_CHECKS.md) | `src/main/java/com/scheduler/health/` | - |
| Structured Logging | ⏸️ TODO | [docs/features/LOGGING.md](./docs/features/LOGGING.md) | `src/main/java/com/scheduler/logging/` | - |
| OpenTelemetry Tracing | ⏸️ TODO | [docs/features/TRACING.md](./docs/features/TRACING.md) | `src/main/java/com/scheduler/tracing/` | - |

### Phase 5: Advanced Features

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Job Dependency DAG | ⏸️ TODO | [docs/features/JOB_DEPENDENCIES.md](./docs/features/JOB_DEPENDENCIES.md) | `src/main/java/com/scheduler/dependencies/` | - |
| Rate Limiting Service | ⏸️ TODO | [docs/features/RATE_LIMITING.md](./docs/features/RATE_LIMITING.md) | `src/main/java/com/scheduler/ratelimit/` | - |
| Graceful Shutdown | ⏸️ TODO | [docs/features/GRACEFUL_SHUTDOWN.md](./docs/features/GRACEFUL_SHUTDOWN.md) | `src/main/java/com/scheduler/lifecycle/` | - |
| Dead Letter Queue | ⏸️ TODO | [docs/features/DEAD_LETTER_QUEUE.md](./docs/features/DEAD_LETTER_QUEUE.md) | `src/main/java/com/scheduler/dlq/` | - |

### Phase 6: Frontend & Documentation

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Angular Project Setup | ⏸️ TODO | [docs/features/FRONTEND_SETUP.md](./docs/features/FRONTEND_SETUP.md) | `scheduler-ui/` | - |
| Job Dashboard | ⏸️ TODO | [docs/features/JOB_DASHBOARD.md](./docs/features/JOB_DASHBOARD.md) | `scheduler-ui/src/app/` | - |
| Cluster Visualization | ⏸️ TODO | [docs/features/CLUSTER_UI.md](./docs/features/CLUSTER_UI.md) | `scheduler-ui/src/app/` | - |
| Metrics Dashboard | ⏸️ TODO | [docs/features/METRICS_UI.md](./docs/features/METRICS_UI.md) | `scheduler-ui/src/app/` | - |

---

## Feature Documentation Index

All feature-specific documentation is located in `docs/features/`. Each document follows a standard template:

- **Purpose**: What problem does this feature solve?
- **Implementation Details**: How is it implemented?
- **Configuration**: Required configuration and properties

---

## Project Structure

```
distributed-job-scheduler/
├── docs/                                    # Feature documentation
│   ├── features/                            # Individual feature docs
│   │   ├── PROJECT_SETUP.md
│   │   ├── DATABASE_SCHEMA.md
│   │   ├── LEADER_ELECTION.md
│   │   └── ...
│   └── diagrams/                            # Architecture diagrams
│       └── ...
├── src/
│   ├── main/
│   │   ├── java/com/scheduler/
│   │   │   ├── SchedulerApplication.java   # Main Spring Boot application
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── DataSourceConfig.java
│   │   │   │   ├── ExecutorConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── domain/                      # Domain entities (JPA)
│   │   │   │   ├── Job.java
│   │   │   │   ├── JobExecution.java
│   │   │   │   ├── JobType.java
│   │   │   │   ├── JobDependency.java
│   │   │   │   ├── SchedulerNode.java
│   │   │   │   ├── AuditLog.java
│   │   │   │   └── DeadLetterQueue.java
│   │   │   ├── repository/                  # JPA repositories
│   │   │   │   ├── JobRepository.java
│   │   │   │   ├── JobExecutionRepository.java
│   │   │   │   ├── JobTypeRepository.java
│   │   │   │   └── ...
│   │   │   ├── service/                     # Business logic
│   │   │   │   ├── JobService.java
│   │   │   │   ├── JobExecutionService.java
│   │   │   │   └── ...
│   │   │   ├── controller/                  # REST API controllers
│   │   │   │   ├── JobController.java
│   │   │   │   ├── ClusterController.java
│   │   │   │   └── MetricsController.java
│   │   │   ├── coordination/                # Leader election & cluster
│   │   │   │   ├── LeaderElectionService.java
│   │   │   │   ├── HeartbeatService.java
│   │   │   │   ├── FencingTokenProvider.java
│   │   │   │   └── ClusterStateService.java
│   │   │   ├── locking/                     # Distributed locks
│   │   │   │   ├── DistributedLockService.java
│   │   │   │   └── RedlockManager.java
│   │   │   ├── executor/                    # Job execution
│   │   │   │   ├── JobScheduler.java
│   │   │   │   ├── JobExecutor.java
│   │   │   │   └── VirtualThreadExecutor.java
│   │   │   ├── retry/                       # Retry logic
│   │   │   │   ├── RetryManager.java
│   │   │   │   └── BackoffStrategy.java
│   │   │   ├── statemachine/                # Job state machine
│   │   │   │   ├── JobStateManager.java
│   │   │   │   └── JobState.java
│   │   │   ├── idempotency/                 # Idempotency handling
│   │   │   │   └── IdempotencyService.java
│   │   │   ├── ratelimit/                   # Rate limiting
│   │   │   │   └── RateLimitService.java
│   │   │   ├── metrics/                     # Custom metrics
│   │   │   │   └── MetricsCollector.java
│   │   │   ├── health/                      # Health indicators
│   │   │   │   ├── LeadershipHealthIndicator.java
│   │   │   │   └── RedisHealthIndicator.java
│   │   │   ├── logging/                     # Logging aspects
│   │   │   │   └── LoggingAspect.java
│   │   │   ├── exception/                   # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── SchedulerException.java
│   │   │   └── dto/                         # Data Transfer Objects
│   │   │       ├── JobRequest.java
│   │   │       ├── JobResponse.java
│   │   │       └── ...
│   │   └── resources/
│   │       ├── application.yml              # Main configuration
│   │       ├── application-dev.yml          # Dev profile
│   │       ├── application-prod.yml         # Production profile
│   │       ├── logback-spring.xml           # Logging configuration
│   │       └── db/
│   │           └── migration/               # Flyway migrations
│   │               ├── V1__create_jobs_table.sql
│   │               ├── V2__create_job_executions_table.sql
│   │               ├── V3__create_scheduler_nodes_table.sql
│   │               └── V4__create_additional_indexes.sql
│   │               └── v2.0/
│   └── test/
│       ├── java/com/scheduler/
│       │   ├── integration/                 # Integration tests
│       │   │   ├── JobExecutionIntegrationTest.java
│       │   │   └── LeaderElectionIntegrationTest.java
│       │   ├── unit/                        # Unit tests
│       │   │   ├── service/
│       │   │   ├── coordination/
│       │   │   └── ...
│       │   └── chaos/                       # Chaos tests
│       │       ├── LeaderFailoverTest.java
│       │       └── NetworkPartitionTest.java
│       └── resources/
│           ├── application-test.yml
│           └── testcontainers.properties
├── scheduler-ui/                            # Angular frontend (Phase 6)
│   ├── src/
│   ├── package.json
│   └── angular.json
├── deployment/                              # Deployment configs
│   ├── docker/
│   │   ├── Dockerfile
│   │   └── docker-compose.yml
│   ├── kubernetes/
│   │   ├── namespace.yaml
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── configmap.yaml
│   │   └── secret.yaml
│   └── prometheus/
│       └── prometheus.yml
├── pom.xml                                  # Maven build configuration
├── README.md                                # Project overview
├── ARCHITECTURE.md                          # Architecture documentation
├── DEVELOPMENT.md                           # This file - development tracker
└── DIAGRAMS_ASCII.md                        # ASCII architecture diagrams
```

---

## Getting Started

### Prerequisites
- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.9+
- Docker & Docker Compose
- Redis 7.2+ (via Docker)
- MySQL 8.0+ (via Docker)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd distributed-job-scheduler
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d redis mysql
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application**
   - API: http://localhost:8080/api
   - Actuator: http://localhost:8080/actuator
   - Health: http://localhost:8080/actuator/health
   - Metrics: http://localhost:8080/actuator/prometheus

### Running Tests

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest=*Test

# Run only integration tests
mvn test -Dtest=*IntegrationTest

# Run with coverage
mvn test jacoco:report
```

### Docker Compose Setup

```bash
# Start all services (Redis, MySQL, Scheduler nodes)
docker-compose up -d

# View logs
docker-compose logs -f scheduler-node-1

# Stop all services
docker-compose down
```

---

## Development Workflow

### Adding a New Feature

1. **Create feature branch**
   ```bash
   git checkout -b feature/feature-name
   ```

2. **Create feature documentation**
   - Create `docs/features/FEATURE_NAME.md`
   - Follow the standard template (see existing docs)

3. **Implement the feature**
   - Write tests first (TDD approach)
   - Implement the feature
   - Add JavaDoc comments

4. **Update DEVELOPMENT.md**
   - Mark feature as IN_PROGRESS
   - Update code location
   - Add completion timestamp when done

5. **Test thoroughly**
   - Unit tests
   - Integration tests
   - Manual testing

6. **Commit and push**
   ```bash
   git add .
   git commit -m "feat: implement feature-name"
   git push origin feature/feature-name
   ```

### Code Style Guidelines

- **Java**: Follow Google Java Style Guide
- **Naming**: Use descriptive names, avoid abbreviations
- **Comments**: JavaDoc for public APIs, inline comments for complex logic
- **Formatting**: Use IDE auto-formatting (IntelliJ IDEA recommended)
- **Logging**: Use SLF4J with structured logging
- **Error Handling**: Use custom exceptions, never swallow exceptions

---

## Current Sprint (Phase 1)

### Sprint Goal
Establish the core infrastructure with database schema, domain entities, and basic job execution capability (single-node).

### Tasks in Progress
- [x] Project structure and Maven setup ✅
- [x] Database schema design with Flyway ✅
- [x] Core domain entities (Job, JobExecution, SchedulerNode) ✅
- [x] JPA repositories ✅
- [x] Basic configuration (application.yml) ✅
- [ ] Coordination layer (leader election, distributed locking)
- [ ] Job service layer
- [ ] REST API controllers
- [ ] Job executor with virtual threads

### Next Steps
1. ✅ Create Maven project structure with dependencies
2. ✅ Set up Flyway for database migrations
3. ✅ Implement core domain entities
4. ✅ Create JPA repositories
5. Implement coordination layer (Week 2)
6. Implement job service layer (Week 3)
7. Create REST API endpoints (Week 4)
8. Implement job executor with virtual threads (Week 3)
8. Write unit and integration tests

---

## Notes & Decisions

### 2026-03-07: Project Initialization & Week 1 Completion

**Architecture Decisions:**
- **Decision**: Use Maven over Gradle for better IDE support and familiarity
- **Decision**: Java 21 for Virtual Threads and modern language features
- **Decision**: Spring Boot 3.2.3 for latest features and security updates
- **Decision**: Interview-Grade architecture (simplified, ~40 classes) over Production-Grade (80+ classes) to focus on distributed systems concepts
- **Decision**: Redisson over Jedis for advanced Redis features (Redlock, etc.)
- **Decision**: Defer observability (Prometheus, Grafana, custom metrics) to Phase 4 to focus on core distributed systems functionality first

**Database & Schema Management:**
- **Decision**: Migrated from Liquibase to Flyway for simpler SQL-based migrations
  - Rationale: SQL is more readable, easier to review in version control, industry standard
  - Created 4 Flyway migrations (V1-V4) for jobs, job_executions, scheduler_nodes tables
  - See: `docs/FLYWAY_MIGRATION_SUMMARY.md`
- **Decision**: Use VARCHAR instead of MySQL ENUM for enum columns
  - Rationale: Database portability, easier migrations, no vendor lock-in
  - Applies to: `status` columns in `jobs` and `job_executions` tables
  - See: `docs/HIBERNATE_ENUM_VALIDATION_FIX.md`
- **Decision**: Set `hibernate.ddl-auto: none` when using Flyway
  - Rationale: Flyway manages schema, Hibernate should not validate
  - Prevents conflicts between Hibernate expectations and actual schema
  - Clean separation of concerns

**Issues Resolved:**
- Fixed duplicate `spring:` YAML keys in `application-dev.yml` and `application-prod.yml`
- Fixed Hibernate schema validation error for enum columns (VARCHAR vs ENUM mismatch)
- Replaced Liquibase references with Flyway in production configuration

**Completed**: Week 1 - Domain + Database Layer (entities, enums, repositories, Flyway migrations)

### 2026-03-07: Week 2 - Coordination Layer Complete

**Coordination & Distributed Systems:**
- **Implemented**: Complete coordination layer with 7 components
  - `SchedulerProperties` - Type-safe configuration binding
  - `RedisConfig` - Redisson client configuration
  - `RedisCoordinationService` - Redis implementation of coordination primitives
  - `LeaderElectionService` - TTL-based leader election with automatic failover
  - `DistributedLockService` - Redlock-based distributed locking
  - `FencingTokenProvider` - Epoch-based fencing tokens for split-brain prevention
  - `HeartbeatService` - Node heartbeat mechanism for failure detection
  - See: `docs/WEEK2_COORDINATION_LAYER.md`

**Key Design Decisions:**
- **Decision**: Use Redis (AP system) over Zookeeper (CP system) for coordination
  - Rationale: Availability > Strong Consistency for job scheduling
  - Lower latency, simpler to operate, built-in TTL support
  - Fencing tokens compensate for weaker consistency guarantees
- **Decision**: TTL-based leases for leader election
  - Rationale: Automatic failover if leader crashes
  - Heartbeat renewal at TTL/3 allows for 2 missed heartbeats
  - Balances fast failover with tolerance for transient network issues
- **Decision**: Epoch-based fencing tokens
  - Rationale: Prevents split-brain scenarios
  - Database validates writes against current epoch
  - Zombie leaders cannot corrupt state after network partitions

**Post-Implementation Verification:**
- **Redisson Watchdog Mechanism Verification** (2026-03-07)
  - Verified watchdog implementation against official Redisson source code
  - **Key Findings:**
    - Default `lockWatchdogTimeout` = 30,000ms (30 seconds) - confirmed in `Config.java` line 95
    - Renewal interval = `lockWatchdogTimeout / 3` = 10,000ms (10 seconds) - confirmed in source code analysis
    - Watchdog enabled when `leaseTime = -1` - confirmed in `RedissonLock.java` lines 175-180
    - Lock TTL is automatically renewed every 10 seconds while the lock holder is alive
  - **Evidence Sources:**
    - Official Redisson GitHub source code (`Config.java`, `RedissonLock.java`, `RedissonBaseLock.java`)
    - Official Redisson documentation (redisson.pro)
    - Official Redisson GitHub Wiki
    - Technical blog with experimental verification and source code analysis
  - **Conclusion:** Implementation in `RedisCoordinationService` is correct and follows Redisson's documented behavior
  - **Reference:** See `docs/REDISSON_WATCHDOG_VERIFICATION.md` for detailed evidence

**Build Status:**
- ✅ `mvn clean compile` - SUCCESS (18 source files compiled)
- ✅ All coordination services implemented and compiling
- ✅ Watchdog mechanism verified against official Redisson source code
- ✅ Ready for Week 3: Execution Layer

**Completed**: Week 2 - Coordination Layer (leader election, distributed locking, fencing tokens, heartbeats)

### 2026-03-08: Week 3 Execution Layer Completion & RETRYING Jobs Bug Fix

**Execution Layer Implementation:**
- ✅ Implemented `JobExecutor` with Java 21 Virtual Threads for high-concurrency job execution
- ✅ Implemented retry logic with exponential backoff and jitter in `RetryManager`
- ✅ Implemented leader-only job polling in `JobScheduler`
- ✅ Implemented fencing token validation to prevent stale/zombie executions
- ✅ Build Status: `mvn clean compile` - SUCCESS (30 source files compiled)

### 2026-03-09: Week 4 REST API Layer Complete

**REST API Implementation:**
- ✅ Implemented all DTOs using Java 21 Records (CreateJobRequest, UpdateJobRequest, JobResponse, etc.)
- ✅ Implemented `JobController` with full CRUD operations and job lifecycle management
- ✅ Implemented `JobExecutionController` for execution history queries
- ✅ Implemented `ClusterController` for cluster status and monitoring
- ✅ Implemented `GlobalExceptionHandler` with standardized error responses
- ✅ Implemented `DtoMapper` for entity-to-DTO conversion
- ✅ Build Status: `mvn clean compile` - SUCCESS (46 source files compiled)

**Key Features:**
- RESTful API design with proper HTTP verbs and status codes
- Comprehensive Bean Validation with field-level error messages
- Pagination support for scalability
- Partial updates (PATCH semantics) for job updates
- Centralized exception handling with consistent error format
- Stateless design for horizontal scaling

**Custom Bean Validation Implementation (2026-03-09):**
- ✅ Created custom `@ValidCronExpression` annotation for cron expression validation
- ✅ Implemented `CronExpressionValidator` using Spring's `CronExpression.parse()` for accuracy
- ✅ Replaced brittle regex validation with robust parser-based validation
- ✅ Updated `CreateJobRequest` and `UpdateJobRequest` to use custom validator
- ✅ Build Status: `mvn clean compile` - SUCCESS (48 source files compiled)

**Design Decisions:**
- **Why custom validator over regex?**
  - Cron expressions have complex rules difficult to express in regex
  - Spring's `CronExpression.parse()` provides accurate validation
  - Same parser used for validation and execution ensures consistency
  - Better error messages with specific parsing failure details
- **Why allow null/blank values?**
  - Cron expressions are optional for one-time jobs
  - Separation of concerns: `@NotBlank` handles required field validation
  - Makes validator reusable for both required and optional fields
- **Integration with JSR-380:**
  - Seamless integration with Jakarta Bean Validation framework
  - Works alongside other validators (`@NotBlank`, `@Size`, `@Pattern`)
  - Global exception handling returns HTTP 400 with field-level errors

**Files Created:**
- `src/main/java/com/scheduler/validation/ValidCronExpression.java` - Custom annotation
- `src/main/java/com/scheduler/validation/CronExpressionValidator.java` - Validator implementation

**Files Modified:**
- `src/main/java/com/scheduler/dto/CreateJobRequest.java` - Uses `@ValidCronExpression`
- `src/main/java/com/scheduler/dto/UpdateJobRequest.java` - Uses `@ValidCronExpression`

**Critical Bug Fix - RETRYING Jobs Never Re-Executed:**
- **Problem**: Jobs in `RETRYING` status were stuck forever because `JobRepository.findDueJobs()` only queried for `status = 'PENDING'`
- **Root Cause**: The polling query excluded RETRYING jobs, breaking the entire retry mechanism
- **Solution**: Modified query to `WHERE j.status IN ('PENDING', 'RETRYING')` to include retry jobs
- **Impact**: Complete retry flow now works end-to-end (FAILED → RETRYING → SCHEDULED → RUNNING → COMPLETED)
- **Documentation**:
  - Detailed analysis: `docs/RETRYING_JOBS_BUG_FIX.md`
  - Visual comparison: `docs/RETRY_FLOW_COMPARISON.md`
  - Quick reference: `docs/RETRYING_JOBS_FIX_SUMMARY.md`
- **Key Learning**: State machines need complete transitions - every non-terminal state needs a way to transition out

**Fencing Token Validation:**
- Implemented safety mechanism to prevent split-brain corruption
- `JobService` validates leader's epoch before allowing job status transitions
- Prevents "zombie" nodes from performing stale writes after losing leadership
- Documentation: `docs/FENCING_TOKEN_VALIDATION.md`

**Orphaned Job Recovery Implementation:**
- **Problem**: Jobs stuck in RUNNING status when nodes crash were never recovered
- **Root Cause**: Lock expiration in Redis doesn't update job status in database
- **Solution**: Implemented `OrphanedJobRecoveryService` with scheduled recovery task
- **How It Works**:
  - Runs every 60 seconds on leader node only
  - Queries for jobs in RUNNING status for longer than 5 minutes
  - Marks stuck jobs as FAILED and schedules retries or moves to dead letter queue
  - Uses deprecated methods (bypasses fencing token validation for recovery)
- **Configuration**: Fully configurable via `application.yml` (interval, threshold, enabled flag)
- **Documentation**:
  - Implementation guide: `docs/ORPHANED_JOB_RECOVERY.md`
  - Quick reference: `docs/ORPHANED_JOB_RECOVERY_SUMMARY.md`
- **Key Learning**: Active recovery (periodic scanning) is needed in addition to passive recovery (lock expiration) because lock expiration alone doesn't update database state

**Completed**: Week 3 - Execution Layer (job execution, retry logic, fencing validation, orphaned job recovery) - 2026-03-08

### Future Decisions to Make
- [ ] Choose between H2 and MySQL for integration tests (leaning towards Testcontainers with MySQL)
- [ ] Decide on OpenTelemetry integration timeline (Phase 4 or later)
- [ ] Determine if we need a separate read model (CQRS) for job history

---

## Useful Commands

```bash
# Verify build environment (recommended first step)
./verify-build.sh        # Linux/Mac
verify-build.bat         # Windows

# Build without tests
mvn clean install -DskipTests

# Build with tests (requires Docker)
mvn clean install

# Run specific test
mvn test -Dtest=JobServiceTest

# Generate JavaDoc
mvn javadoc:javadoc

# Check for dependency updates
mvn versions:display-dependency-updates

# Run application with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package as JAR
mvn clean package

# Build Docker image
docker build -t scheduler:latest -f deployment/docker/Dockerfile .
```

## Troubleshooting

If you encounter build errors, see [docs/TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md) for:
- Common build issues and solutions
- Environment verification checklist
- Step-by-step debugging guide

---

## Contact & Support

For questions or issues during development, refer to:
- Architecture documentation: [ARCHITECTURE.md](./ARCHITECTURE.md)
- Feature documentation: [docs/features/](./docs/features/)
- ASCII diagrams: [DIAGRAMS_ASCII.md](./DIAGRAMS_ASCII.md)

---

**Last Updated**: 2026-03-09 (Week 4 Complete - REST API Layer & Custom Bean Validation)
**Updated By**: Development Team
**Next Review**: After Phase 1 completion

- **Usage Examples**: Code examples and API usage
- **Testing Approach**: How to test this feature
- **Known Limitations**: Current constraints and future improvements

### Core Features
- [Project Setup](./docs/features/PROJECT_SETUP.md) - Maven configuration, dependencies, project structure
- [Week 1 Summary](./docs/WEEK1_SUMMARY.md) - Domain + Database Layer implementation
- [Database Schema](./docs/WEEK1_SUMMARY.md#database-schema) - Flyway migrations, table design, indexes
- [Domain Model](./docs/WEEK1_SUMMARY.md#domain-entities) - JPA entities, enums, repositories

### Distributed Systems Features
- [Leader Election](./docs/features/LEADER_ELECTION.md) - Redis-based leader election algorithm
- [Distributed Locks](./docs/features/DISTRIBUTED_LOCKS.md) - Redlock implementation
- [Fencing Tokens](./docs/features/FENCING_TOKENS.md) - Split-brain prevention

### Observability Features
- [Metrics](./docs/features/METRICS.md) - Prometheus metrics and custom collectors
- [Health Checks](./docs/features/HEALTH_CHECKS.md) - Custom health indicators
- [Logging](./docs/features/LOGGING.md) - Structured logging with MDC

---

## Technology Stack

**Backend:**
- Java 21 (LTS) - Virtual Threads, Records, Pattern Matching
- Spring Boot 3.2.3
- Spring Data JPA (Hibernate 6.4)
- Redisson 3.27.0 (Redis client)
- Flyway 10.8.1 (Database migrations)

**Coordination & Caching:**
- Redis 7.2+ (Leader election, distributed locks, idempotency)

**Database:**
- MySQL 8.0+ (Persistent job storage)

**Observability:**
- Micrometer 1.12+ (Metrics abstraction)
- Prometheus (Metrics collection)
- Logback (Logging)
- OpenTelemetry 1.35+ (Distributed tracing - optional)

**Build & Testing:**
- Maven 3.9+
- JUnit 5 (Jupiter)
- Testcontainers 1.19+ (Integration tests)
- Mockito 5+ (Unit tests)

**DevOps:**
- Docker & Docker Compose
- Kubernetes 1.28+


