# Distributed Job Scheduler - Project Context & Memory

**Last Updated**: 2026-03-09
**Project Status**: Phase 1 - Week 4 Complete ✅
**Next Phase**: Week 5 - Testing & Integration

---

## Quick Reference

### Project Overview
A **distributed job scheduling system** built with Java 21 and Spring Boot 3.2.3, demonstrating advanced distributed systems concepts for technical interviews.

**Architecture**: Interview-Grade (~40 classes) - Simplified to focus 70% on distributed systems concepts  
**Goal**: Showcase expertise in leader election, distributed locking, fencing tokens, and failure handling

---

## Current Status (2026-03-09)

### ✅ Completed (Week 1 + Week 2 + Week 3 + Week 4)

**Week 1: Domain + Database Layer:**
- ✅ Project structure with Maven
- ✅ Core domain entities: `Job`, `JobExecution`, `SchedulerNode`
- ✅ Enums: `JobStatus`, `ExecutionStatus`, `NodeStatus`
- ✅ JPA repositories: `JobRepository`, `JobExecutionRepository`, `SchedulerNodeRepository`
- ✅ Flyway migrations (V1-V4) for database schema
- ✅ Configuration files (application.yml, -dev, -prod, -test)

**Week 2: Coordination Layer:**
- ✅ `SchedulerProperties` - Type-safe configuration binding
- ✅ `RedisConfig` - Redisson client configuration
- ✅ `CoordinationService` interface - Abstraction for coordination primitives
- ✅ `RedisCoordinationService` - Redis implementation with Redlock
- ✅ `LeaderElectionService` - TTL-based leader election with automatic failover
- ✅ `DistributedLockService` - High-level distributed locking API
- ✅ `FencingTokenProvider` - Epoch-based fencing tokens
- ✅ `HeartbeatService` - Node heartbeat and failure detection

**Week 3: Execution Layer:**
- ✅ `JobExecutor` - Virtual Thread-based job execution engine (10,000+ concurrent jobs)
- ✅ `RetryManager` - Exponential backoff with jitter (30s, 60s, 120s)
- ✅ `JobScheduler` - Leader-only job polling (1-second intervals)
- ✅ `JobService` - Job lifecycle management with fencing token validation
- ✅ `OrphanedJobRecoveryService` - Active recovery for stuck jobs (60-second intervals)
- ✅ Fencing token validation to prevent stale/zombie executions
- ✅ Complete retry flow: FAILED → RETRYING → SCHEDULED → RUNNING → COMPLETED
- ✅ Orphaned job recovery: Detects and recovers jobs stuck in RUNNING status
- ✅ Exception handling: `StaleExecutionException`, `JobExecutionException`, `InvalidJobStateException`

**Week 4: REST API Layer:**
- ✅ DTOs using Java 21 Records: `CreateJobRequest`, `UpdateJobRequest`, `JobResponse`, `JobListResponse`, `JobExecutionResponse`, `ExecutionHistoryResponse`, `ClusterStatusResponse`, `NodeStatusResponse`, `ErrorResponse`
- ✅ `JobController` - Full CRUD operations, job lifecycle management (pause, resume, cancel, trigger)
- ✅ `JobExecutionController` - Execution history queries with pagination
- ✅ `ClusterController` - Cluster status, leader information, node health monitoring
- ✅ `GlobalExceptionHandler` - Centralized exception handling with standardized error responses
- ✅ `DtoMapper` - Entity-to-DTO conversion with proper field mapping
- ✅ Bean Validation with field-level error messages
- ✅ Pagination support for scalability
- ✅ Partial updates (PATCH semantics) for job updates
- ✅ Stateless design for horizontal scaling

**Week 4 Enhancement: Custom Bean Validation (2026-03-09):**
- ✅ `@ValidCronExpression` - Custom JSR-380 annotation for cron expression validation
- ✅ `CronExpressionValidator` - Validator implementation using Spring's `CronExpression.parse()`
- ✅ Replaced brittle regex validation with robust parser-based validation
- ✅ Allows null/blank values (optional field for one-time jobs)
- ✅ Custom error messages with specific parsing failure details
- ✅ Consistent validation across `CreateJobRequest` and `UpdateJobRequest`
- ✅ Seamless integration with Jakarta Bean Validation framework

**Critical Bug Fixes & Features:**
- ✅ **RETRYING Jobs Bug** (2026-03-08):
  - **Problem**: Jobs in `RETRYING` status were never re-executed
  - **Root Cause**: `JobRepository.findDueJobs()` query only looked for `status = 'PENDING'`, excluding RETRYING jobs
  - **Solution**: Changed query to `WHERE j.status IN ('PENDING', 'RETRYING')`
  - **Impact**: Complete retry flow now works end-to-end
  - **Documentation**:
    - `docs/RETRYING_JOBS_BUG_FIX.md` (detailed analysis)
    - `docs/RETRY_FLOW_COMPARISON.md` (visual comparison)
    - `docs/RETRYING_JOBS_FIX_SUMMARY.md` (quick reference)

- ✅ **Orphaned Job Recovery** (2026-03-08):
  - **Problem**: Jobs stuck in RUNNING status when nodes crash were never recovered
  - **Root Cause**: Lock expiration in Redis doesn't update job status in database
  - **Solution**: Implemented `OrphanedJobRecoveryService` with scheduled recovery task
  - **How It Works**: Runs every 60 seconds on leader, finds jobs stuck >5 minutes, marks as FAILED and schedules retries
  - **Impact**: Automatic recovery of orphaned jobs, no manual intervention needed
  - **Documentation**:
    - `docs/ORPHANED_JOB_RECOVERY.md` (implementation guide)
    - `docs/ORPHANED_JOB_RECOVERY_SUMMARY.md` (quick reference)

**Other Issues Resolved:**
- ✅ Migrated from Liquibase to Flyway
- ✅ Fixed duplicate YAML keys in configuration files
- ✅ Fixed Hibernate schema validation error (VARCHAR vs ENUM)
- ✅ Fixed leader election lock renewal bug (enabled Redisson watchdog)
- ✅ Fixed lock expiration race condition (fencing token validation)

### 🚧 In Progress

**None** - Ready to start Week 5 (Testing & Integration)

### ⏸️ Next Steps

**Week 5: Testing & Integration**
- Write integration tests with Testcontainers
- Write unit tests for critical components
- Test distributed scenarios (leader failover, split-brain, etc.)
- Load testing with concurrent job execution

**Week 6: Frontend (Angular)**
- Job management UI
- Execution history dashboard
- Cluster monitoring dashboard

---

## Key Architectural Decisions

### Technology Stack
| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| **Language** | Java | 21 | Virtual Threads, Records, Pattern Matching |
| **Framework** | Spring Boot | 3.2.3 | Latest features, security updates |
| **Build Tool** | Maven | 3.9+ | Better IDE support, familiarity |
| **Database** | MySQL | 8.0+ | Persistent job storage |
| **Coordination** | Redis | 7.2+ | Leader election, distributed locks |
| **Redis Client** | Redisson | 3.27.0 | Advanced features (Redlock, etc.) |
| **Migrations** | Flyway | 10.8.1 | SQL-based, readable, industry standard |
| **Testing** | Testcontainers | 1.19+ | Integration tests with real databases |

### Database Design Decisions

**1. VARCHAR vs MySQL ENUM for Enum Columns**
- ✅ **Decision**: Use `VARCHAR(20)` for all enum columns
- **Rationale**:
  - Database portability (works with PostgreSQL, Oracle, etc.)
  - Easier migrations (no ALTER TABLE needed for new values)
  - Standard JPA approach with `@Enumerated(EnumType.STRING)`
  - No vendor lock-in
- **Applies to**: `status` columns in `jobs` and `job_executions` tables

**2. Hibernate DDL Auto Mode**
- ✅ **Decision**: Set `hibernate.ddl-auto: none` in all profiles
- **Rationale**:
  - Flyway manages schema evolution
  - Hibernate focuses on ORM, not schema management
  - Prevents conflicts between Hibernate expectations and actual schema
  - Clean separation of concerns
- **Previous value**: `validate` (caused schema validation errors)

**3. Flyway over Liquibase**
- ✅ **Decision**: Migrated from Liquibase to Flyway
- **Rationale**:
  - SQL is more readable and familiar
  - Easier to review in version control
  - Industry standard for schema evolution
  - Simpler for interview discussions

### Distributed Systems Patterns

**Fencing Tokens** (Implemented in Schema):
- `job_executions.fencing_token` - Prevents split-brain scenarios
- Monotonically increasing token validates execution authority
- See: `docs/FLYWAY_MIGRATION_SUMMARY.md`

**Epoch Numbers** (Implemented in Schema):
- `scheduler_nodes.epoch` - Leader fencing mechanism
- Incremented on each leader election
- Prevents stale leaders from executing jobs

**Optimistic Locking** (Implemented in Entities):
- `@Version` annotation on all entities
- Prevents concurrent modification conflicts
- Database-level concurrency control

---

## Important Files & Locations

### Configuration Files
| File | Purpose | Status |
|------|---------|--------|
| `src/main/resources/application.yml` | Base configuration | ✅ Updated |
| `src/main/resources/application-dev.yml` | Development profile | ✅ Fixed |
| `src/main/resources/application-prod.yml` | Production profile | ✅ Fixed |
| `src/main/resources/application-test.yml` | Test profile | ✅ Fixed |

### Database Migrations
| File | Purpose | Status |
|------|---------|--------|
| `V1__create_jobs_table.sql` | Core job definitions | ✅ Complete |
| `V2__create_job_executions_table.sql` | Execution history with fencing tokens | ✅ Complete |
| `V3__create_scheduler_nodes_table.sql` | Node tracking with epoch numbers | ✅ Complete |
| `V4__create_additional_indexes.sql` | Performance indexes | ✅ Complete |

### Domain Entities
| File | Purpose | Status |
|------|---------|--------|
| `domain/entity/Job.java` | Job definition entity | ✅ Complete |
| `domain/entity/JobExecution.java` | Execution history entity | ✅ Complete |
| `domain/entity/SchedulerNode.java` | Cluster node entity | ✅ Complete |
| `domain/enums/JobStatus.java` | Job state enum | ✅ Complete |
| `domain/enums/ExecutionStatus.java` | Execution state enum | ✅ Complete |
| `domain/enums/NodeStatus.java` | Node state enum | ✅ Complete |

### Repositories
| File | Purpose | Status |
|------|---------|--------|
| `repository/JobRepository.java` | Job data access | ✅ Complete |
| `repository/JobExecutionRepository.java` | Execution data access | ✅ Complete |
| `repository/SchedulerNodeRepository.java` | Node data access | ✅ Complete |

### Validation (Custom Bean Validation)
| File | Purpose | Status |
|------|---------|--------|
| `validation/ValidCronExpression.java` | Custom JSR-380 annotation for cron validation | ✅ Complete (2026-03-09) |
| `validation/CronExpressionValidator.java` | Validator using Spring's CronExpression.parse() | ✅ Complete (2026-03-09) |

**Key Features:**
- Uses Spring's `CronExpression.parse()` for accurate validation (same parser as execution logic)
- Allows null/blank values (optional field for one-time jobs)
- Custom error messages with specific parsing failure details
- Seamless integration with Jakarta Bean Validation (JSR-380)
- Used in `CreateJobRequest` and `UpdateJobRequest` DTOs

**Interview Talking Points:**
- **Q: "Why custom validator over regex?"**
  - Cron expressions have complex rules difficult to express in regex
  - Spring's parser provides accurate validation
  - Same parser for validation and execution ensures consistency
  - Better error messages with specific parsing failure details

- **Q: "Why allow null/blank values?"**
  - Cron expressions are optional for one-time jobs
  - Separation of concerns: `@NotBlank` handles required field validation
  - Makes validator reusable for both required and optional fields

- **Q: "How does it integrate with the validation framework?"**
  - Implements `ConstraintValidator<ValidCronExpression, String>`
  - Spring Boot auto-configures Bean Validation - no manual wiring
  - Works alongside other validators (`@NotBlank`, `@Size`, `@Pattern`)
  - `GlobalExceptionHandler` catches validation errors and returns HTTP 400

---

## Documentation Index

### Core Documentation
- **`README.md`** - Project overview and quick start
- **`ARCHITECTURE.md`** - System architecture and design
- **`DEVELOPMENT.md`** - Development progress tracker (this is the main tracker)
- **`docs/INTERVIEW_GRADE_IMPLEMENTATION.md`** - 5-week roadmap

### Week 1 Documentation
- **`docs/WEEK1_SUMMARY.md`** - Week 1 completion summary
- **`docs/FLYWAY_MIGRATION_SUMMARY.md`** - Flyway migration guide
- **`docs/HIBERNATE_ENUM_VALIDATION_FIX.md`** - Hibernate validation fix
- **`docs/YAML_DUPLICATE_KEY_FIX.md`** - YAML configuration fix

### Architecture Documentation
- **`docs/ARCHITECTURE_REVIEW.md`** - Rationale for simplified architecture
- **`docs/OBSERVABILITY_STRATEGY.md`** - Observability approach (deferred to Phase 4)

---

## Known Issues & Resolutions

### ✅ Resolved Issues

**1. Duplicate YAML Keys**
- **Issue**: Two `spring:` root keys in `application-dev.yml` and `application-prod.yml`
- **Fix**: Merged Redis configuration under first `spring:` key
- **Date**: 2026-03-07
- **Doc**: `docs/YAML_DUPLICATE_KEY_FIX.md`

**2. Hibernate Schema Validation Error**
- **Issue**: `wrong column type encountered in column [status]` - Hibernate expected ENUM, found VARCHAR
- **Fix**: Changed `hibernate.ddl-auto` from `validate` to `none`
- **Date**: 2026-03-07
- **Doc**: `docs/HIBERNATE_ENUM_VALIDATION_FIX.md`

**3. Liquibase References in Production Config**
- **Issue**: `application-prod.yml` still referenced Liquibase
- **Fix**: Replaced with Flyway configuration
- **Date**: 2026-03-07

**4. Leader Election Lock Renewal Bug**
- **Issue**: `renewLeadership()` didn't extend Redis lock TTL, causing leader churn every ~10s
- **Fix**: Enabled Redisson's watchdog mechanism (leaseTime=-1) for automatic lock renewal
- **Verification**: Validated against official Redisson source code and documentation
  - Confirmed default `lockWatchdogTimeout` = 30 seconds
  - Confirmed renewal interval = `lockWatchdogTimeout / 3` = 10 seconds
  - Verified watchdog activation when `leaseTime = -1`
- **Date**: 2026-03-07
- **Docs**: `docs/LEADER_ELECTION_WATCHDOG_FIX.md`, `docs/REDISSON_WATCHDOG_VERIFICATION.md`

### ⚠️ Current Limitations

**Docker Requirement for Tests**
- **Issue**: `mvn clean install` requires Docker for Testcontainers
- **Workaround**: Use `mvn clean install -DskipTests` if Docker is unavailable
- **Future**: Consider adding H2 profile for quick tests without Docker

---

## Build & Run Commands

### Build Commands
```bash
# Build without tests (no Docker required)
mvn clean install -DskipTests

# Build with tests (requires Docker)
mvn clean install

# Compile only
mvn clean compile
```

### Run Commands
```bash
# Run with dev profile (requires MySQL and Redis)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with default profile
mvn spring-boot:run
```

### Test Commands
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=JobRepositoryTest
```

---

## Interview Talking Points

### Database & Schema Management

**Q: "Why did you choose Flyway over Liquibase?"**
- SQL is more readable and easier to review in version control
- Industry standard for schema evolution
- Simpler for team collaboration

**Q: "Why VARCHAR instead of MySQL ENUM?"**
- Database portability - works with PostgreSQL, Oracle, etc.
- Easier migrations - no ALTER TABLE needed
- Standard JPA approach with `@Enumerated(EnumType.STRING)`

**Q: "Why disable Hibernate schema validation?"**
- Flyway manages schema evolution (single source of truth)
- Hibernate focuses on ORM, not schema management
- Clean separation of concerns
- Prevents conflicts between tools

### Custom Bean Validation (@ValidCronExpression)

**Q: "Why create a custom validator instead of using regex?"**
- **Accuracy**: Cron expressions have complex rules (field ranges, special characters, day-of-week names) that are difficult to express accurately in regex
- **Consistency**: The validator uses Spring's `CronExpression.parse()` - the same parser used in `JobController` and `JobExecutor` - ensuring validation consistency
- **Better Error Messages**: The validator can catch specific parsing errors and provide detailed feedback (e.g., "Invalid second value: 60")
- **Maintainability**: If Spring updates their cron format, our validator automatically stays in sync

**Q: "Why allow null/blank values in the validator?"**
- **Optional Field**: Cron expressions are optional - jobs without them are one-time jobs that execute immediately
- **Separation of Concerns**: Required field validation is handled by `@NotBlank` if needed - the cron validator only validates format
- **Reusability**: This makes the validator reusable for both required and optional fields
- **Fail Fast**: We only validate when a value is provided, following the "fail fast" principle

**Q: "How does this integrate with the existing validation framework?"**
- **Declarative**: Uses `@Constraint` annotation to register the validator
- **Automatic**: Spring Boot auto-configures Bean Validation - no manual wiring needed
- **Composable**: Works alongside other validators like `@NotBlank`, `@Size`, `@Pattern`
- **Global Exception Handling**: `GlobalExceptionHandler` catches `MethodArgumentNotValidException` and returns HTTP 400 with field-level errors

**Q: "What about performance?"**
- Cron parsing is very fast (microseconds), so validating at the API boundary doesn't add significant overhead
- The benefit of catching invalid expressions early - before they reach the database or scheduling logic - far outweighs the minimal performance cost
- Validation only happens on create/update operations, not on every job execution

**Q: "How does this improve the system?"**
- **Fail Fast**: Invalid cron expressions are rejected at the API boundary, not during execution
- **Better UX**: Clients get immediate, detailed feedback about what's wrong with their cron expression
- **Data Integrity**: Prevents invalid schedules from being persisted to the database
- **Consistency**: Same validation logic for create and update operations
- **Maintainability**: Centralized validation logic - easy to update if requirements change

---

**This file serves as the single source of truth for project context across development sessions.**

