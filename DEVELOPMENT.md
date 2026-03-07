# Distributed Job Scheduler - Development Progress Tracker

**Project Start Date**: 2026-03-07
**Current Phase**: Phase 1 - Core Infrastructure
**Status**: 🚧 IN PROGRESS

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
| Database Schema Design | ⏸️ TODO | [docs/features/DATABASE_SCHEMA.md](./docs/features/DATABASE_SCHEMA.md) | `src/main/resources/db/changelog/` | - |
| Core Domain Entities | ⏸️ TODO | [docs/features/DOMAIN_MODEL.md](./docs/features/DOMAIN_MODEL.md) | `src/main/java/com/scheduler/domain/` | - |
| JPA Repositories | ⏸️ TODO | [docs/features/DATA_ACCESS.md](./docs/features/DATA_ACCESS.md) | `src/main/java/com/scheduler/repository/` | - |
| Basic Configuration | ⏸️ TODO | [docs/features/CONFIGURATION.md](./docs/features/CONFIGURATION.md) | `src/main/resources/` | - |
| Job Service Layer | ⏸️ TODO | [docs/features/JOB_SERVICE.md](./docs/features/JOB_SERVICE.md) | `src/main/java/com/scheduler/service/` | - |
| REST API Controllers | ⏸️ TODO | [docs/features/REST_API.md](./docs/features/REST_API.md) | `src/main/java/com/scheduler/controller/` | - |
| Single-Node Job Executor | ⏸️ TODO | [docs/features/JOB_EXECUTOR.md](./docs/features/JOB_EXECUTOR.md) | `src/main/java/com/scheduler/executor/` | - |

### Phase 2: Leader Election & Failover

| Feature | Status | Documentation | Code Location | Completed |
|---------|--------|---------------|---------------|-----------|
| Redis Configuration | ⏸️ TODO | [docs/features/REDIS_SETUP.md](./docs/features/REDIS_SETUP.md) | `src/main/java/com/scheduler/config/` | - |
| Leader Election Service | ⏸️ TODO | [docs/features/LEADER_ELECTION.md](./docs/features/LEADER_ELECTION.md) | `src/main/java/com/scheduler/coordination/` | - |
| Heartbeat Mechanism | ⏸️ TODO | [docs/features/HEARTBEAT.md](./docs/features/HEARTBEAT.md) | `src/main/java/com/scheduler/coordination/` | - |
| Fencing Token Provider | ⏸️ TODO | [docs/features/FENCING_TOKENS.md](./docs/features/FENCING_TOKENS.md) | `src/main/java/com/scheduler/coordination/` | - |
| Cluster State Manager | ⏸️ TODO | [docs/features/CLUSTER_STATE.md](./docs/features/CLUSTER_STATE.md) | `src/main/java/com/scheduler/coordination/` | - |

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
- **Decision**: Use Maven over Gradle for better IDE support and familiarity
- **Decision**: Java 21 for Virtual Threads and modern language features
- **Decision**: Spring Boot 3.2.3 for latest features and security updates
- **Decision**: Flyway over Liquibase for simpler SQL-based migrations
- **Decision**: Redisson over Jedis for advanced Redis features (Redlock, etc.)
- **Decision**: Defer observability (Prometheus, Grafana, custom metrics) to Phase 4 to focus on core distributed systems functionality first
- **Decision**: Interview-Grade architecture (simplified, ~40 classes) over Production-Grade (80+ classes) to focus on distributed systems concepts
- **Completed**: Week 1 - Domain + Database Layer (entities, enums, repositories, Flyway migrations)

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

**Last Updated**: 2026-03-07
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


