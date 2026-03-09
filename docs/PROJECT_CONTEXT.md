# Distributed Job Scheduler - Project Context & Memory

**Last Updated**: 2026-03-08
**Project Status**: Phase 1 - Week 3 Complete ✅
**Next Phase**: Week 4 - REST API Layer

---

## Quick Reference

### Project Overview
A **distributed job scheduling system** built with Java 21 and Spring Boot 3.2.3, demonstrating advanced distributed systems concepts for technical interviews.

**Architecture**: Interview-Grade (~40 classes) - Simplified to focus 70% on distributed systems concepts  
**Goal**: Showcase expertise in leader election, distributed locking, fencing tokens, and failure handling

---

## Current Status (2026-03-08)

### ✅ Completed (Week 1 + Week 2 + Week 3)

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

**None** - Ready to start Week 4 (REST API Layer)

### ⏸️ Next Steps

**Week 3: Execution Layer**
- Implement `JobService` for job management
- Implement `JobExecutionService` for execution tracking
- Implement `JobExecutor` with virtual threads
- Implement `RetryManager` for failed jobs

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

---

**This file serves as the single source of truth for project context across development sessions.**

