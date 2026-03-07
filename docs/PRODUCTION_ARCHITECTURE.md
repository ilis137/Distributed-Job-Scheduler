# Production-Grade Architecture - Distributed Job Scheduler

**Status**: 🚧 Implementation in Progress
**Target**: Enterprise-level, production-ready system
**Last Updated**: 2026-03-07

---

## Architecture Overview

This document outlines the production-grade architecture for the Distributed Job Scheduler, designed to meet enterprise standards and pass senior/staff engineer code reviews.

---

## Layered Architecture

### 1. **API Layer** (`com.scheduler.api`)
**Responsibility**: HTTP request/response handling, API versioning, input validation

**Components**:
- `v1/controller/` - REST controllers (versioned)
- `dto/request/` - Request DTOs
- `dto/response/` - Response DTOs
- `mapper/` - DTO ↔ Domain entity mappers
- `validator/` - Custom validators
- `filter/` - Request/response filters
- `interceptor/` - Request interceptors

**Principles**:
- Controllers are thin, delegate to services
- DTOs separate from domain entities
- Input validation at API boundary
- API versioning for backward compatibility

### 2. **Service Layer** (`com.scheduler.service`)
**Responsibility**: Business logic, orchestration, transaction management

**Components**:
- `job/` - Job management services
- `execution/` - Job execution services
- `scheduling/` - Scheduling logic
- `notification/` - Notification services

**Principles**:
- Transactional boundaries
- Business rule enforcement
- Service composition
- No direct HTTP/database dependencies

### 3. **Domain Layer** (`com.scheduler.domain`)
**Responsibility**: Core business entities, domain logic, value objects

**Components**:
- `entity/` - JPA entities
- `valueobject/` - Value objects (immutable)
- `event/` - Domain events
- `specification/` - Business rules

**Principles**:
- Rich domain model
- Encapsulation of business logic
- Domain events for cross-cutting concerns
- No framework dependencies (pure Java)

### 4. **Infrastructure Layer** (`com.scheduler.infrastructure`)
**Responsibility**: External integrations, persistence, messaging

**Components**:
- `persistence/repository/` - JPA repositories
- `persistence/entity/` - JPA entity mappings
- `cache/` - Redis cache implementation
- `messaging/` - Event publishing
- `external/` - External API clients

**Principles**:
- Adapter pattern for external systems
- Repository pattern for data access
- Infrastructure concerns isolated

### 5. **Coordination Layer** (`com.scheduler.coordination`)
**Responsibility**: Distributed systems coordination (leader election, locking)

**Components**:
- `election/` - Leader election
- `locking/` - Distributed locks
- `heartbeat/` - Heartbeat mechanism
- `fencing/` - Fencing tokens
- `cluster/` - Cluster state management

**Principles**:
- Distributed systems patterns
- Fault tolerance
- Split-brain prevention

### 6. **Configuration Layer** (`com.scheduler.config`)
**Responsibility**: Application configuration, bean definitions

**Components**:
- `DatabaseConfig` - Database connection pooling
- `RedisConfig` - Redis configuration
- `SecurityConfig` - Security setup
- `ExecutorConfig` - Thread pool configuration
- `ObservabilityConfig` - Metrics, tracing (Phase 4)

### 7. **Security Layer** (`com.scheduler.security`)
**Responsibility**: Authentication, authorization, audit

**Components**:
- `authentication/` - JWT authentication
- `authorization/` - RBAC implementation
- `audit/` - Audit logging
- `encryption/` - Data encryption

### 8. **Common Layer** (`com.scheduler.common`)
**Responsibility**: Shared utilities, constants, exceptions

**Components**:
- `exception/` - Custom exception hierarchy
- `util/` - Utility classes
- `constant/` - Application constants
- `logging/` - Logging utilities

---

## Package Structure

```
com.scheduler/
├── SchedulerApplication.java
│
├── api/                                    # API Layer
│   ├── v1/
│   │   ├── controller/
│   │   │   ├── JobController.java
│   │   │   ├── JobExecutionController.java
│   │   │   ├── ClusterController.java
│   │   │   └── HealthController.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   ├── CreateJobRequest.java
│   │   │   │   ├── UpdateJobRequest.java
│   │   │   │   └── ExecuteJobRequest.java
│   │   │   └── response/
│   │   │       ├── JobResponse.java
│   │   │       ├── JobExecutionResponse.java
│   │   │       ├── ClusterStatusResponse.java
│   │   │       └── ErrorResponse.java
│   │   ├── mapper/
│   │   │   ├── JobMapper.java
│   │   │   └── JobExecutionMapper.java

---

## Design Patterns & Principles

### SOLID Principles
- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification
- **Liskov Substitution**: Subtypes must be substitutable
- **Interface Segregation**: Many specific interfaces > one general
- **Dependency Inversion**: Depend on abstractions, not concretions

### Design Patterns Used
1. **Repository Pattern**: Data access abstraction
2. **Service Layer Pattern**: Business logic encapsulation
3. **DTO Pattern**: API/Domain separation
4. **Mapper Pattern**: Object transformation
5. **Strategy Pattern**: Retry strategies, backoff algorithms
6. **State Pattern**: Job state machine
7. **Observer Pattern**: Domain events
8. **Factory Pattern**: Object creation
9. **Adapter Pattern**: External system integration
10. **Circuit Breaker Pattern**: Fault tolerance

---

## Error Handling Strategy

### Exception Hierarchy

```
SchedulerException (abstract)
├── BusinessException (4xx errors)
│   ├── JobNotFoundException
│   ├── InvalidJobStateException
│   ├── DuplicateJobException
│   └── ValidationException
├── TechnicalException (5xx errors)
│   ├── LeaderElectionException
│   ├── LockAcquisitionException
│   ├── DatabaseException
│   └── RedisConnectionException
└── SecurityException (401/403 errors)
    ├── AuthenticationException
    └── AuthorizationException
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(JobNotFoundException ex) {
        return ResponseEntity.status(404).body(
            ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(404)
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .correlationId(MDC.get("correlationId"))
                .build()
        );
    }

    // ... other handlers
}
```

---

## Security Architecture

### Authentication Flow

```
1. Client → POST /api/v1/auth/login {username, password}
2. Server → Validate credentials
3. Server → Generate JWT token
4. Server → Return {accessToken, refreshToken, expiresIn}
5. Client → Store tokens securely
6. Client → Include in requests: Authorization: Bearer <token>
7. Server → Validate JWT on each request
```

### Authorization (RBAC)

**Roles**:
- `ADMIN`: Full access (create, update, delete jobs, manage cluster)
- `OPERATOR`: Execute jobs, view status
- `VIEWER`: Read-only access

**Permissions**:
```java
@PreAuthorize("hasRole('ADMIN')")
public JobResponse createJob(CreateJobRequest request) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public void executeJob(Long jobId) { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
public JobResponse getJob(Long jobId) { ... }
```

### Audit Logging

All mutations are audited:
```java
@Audited
public class Job {
    // Automatically logs: who, when, what changed
}
```

---

## API Design Standards

### Versioning Strategy

**URL-based versioning**: `/api/v1/jobs`

**Rationale**:
- Clear and explicit
- Easy to route
- Supports multiple versions simultaneously

### RESTful Conventions

```
GET    /api/v1/jobs              - List all jobs (paginated)
GET    /api/v1/jobs/{id}         - Get job by ID
POST   /api/v1/jobs              - Create new job
PUT    /api/v1/jobs/{id}         - Update job (full)
PATCH  /api/v1/jobs/{id}         - Update job (partial)
DELETE /api/v1/jobs/{id}         - Delete job
POST   /api/v1/jobs/{id}/execute - Execute job (action)
GET    /api/v1/jobs/{id}/executions - Get execution history
```

### Response Format

**Success Response**:
```json
{
  "data": {
    "id": 123,
    "name": "daily-report",
    "status": "PENDING"
  },
  "metadata": {
    "timestamp": "2026-03-07T10:30:00Z",
    "correlationId": "abc-123-def"
  }
}
```

**Error Response**:
```json
{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Invalid cron expression",
    "details": [
      {
        "field": "cronExpression",
        "message": "Must be valid cron format"
      }
    ],
    "timestamp": "2026-03-07T10:30:00Z",
    "correlationId": "abc-123-def",
    "path": "/api/v1/jobs"
  }
}
```

### Pagination

```
GET /api/v1/jobs?page=0&size=20&sort=createdAt,desc

Response:
{
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

## Validation Strategy

### Bean Validation (JSR-380)

```java
public class CreateJobRequest {

    @NotBlank(message = "Job name is required")
    @Size(min = 3, max = 100, message = "Name must be 3-100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Only alphanumeric, dash, underscore")
    private String name;

    @NotBlank(message = "Cron expression is required")
    @CronExpression(message = "Invalid cron expression")
    private String cronExpression;

    @NotNull(message = "Payload is required")
    @Valid
    private JobPayload payload;

    @Min(value = 0, message = "Max retries must be >= 0")
    @Max(value = 10, message = "Max retries must be <= 10")
    private Integer maxRetries = 3;
}
```

### Custom Validators

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronExpressionValidator.class)
public @interface CronExpression {
    String message() default "Invalid cron expression";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class CronExpressionValidator implements ConstraintValidator<CronExpression, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        try {
            CronParser.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

## Logging Strategy

### MDC (Mapped Diagnostic Context)

```java
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        MDC.put("userId", getCurrentUserId());
        MDC.put("nodeId", getNodeId());

        try {
            response.setHeader("X-Correlation-ID", correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Structured Logging

```java
@Slf4j
public class JobServiceImpl implements JobService {

    public JobResponse createJob(CreateJobRequest request) {
        log.info("Creating job: name={}, cron={}",
                 request.getName(),
                 request.getCronExpression());

        try {
            Job job = jobRepository.save(mapper.toEntity(request));

            log.info("Job created successfully: jobId={}, name={}",
                     job.getId(),
                     job.getName());

            return mapper.toResponse(job);
        } catch (Exception e) {
            log.error("Failed to create job: name={}, error={}",
                      request.getName(),
                      e.getMessage(),
                      e);
            throw new JobCreationException("Failed to create job", e);
        }
    }
}
```

### Log Levels

- **ERROR**: System errors, exceptions
- **WARN**: Retries, degraded performance
- **INFO**: Business events (job created, executed)
- **DEBUG**: Detailed flow (dev/staging only)
- **TRACE**: Very detailed (dev only)

---

## Database Design

### Connection Pooling (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: SchedulerHikariPool
```

### Transaction Management

```java
@Service
@Transactional
public class JobServiceImpl implements JobService {

    @Transactional(readOnly = true)
    public JobResponse getJob(Long id) {
        // Read-only transaction
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAuditLog(AuditLog log) {
        // New transaction (even if parent fails)
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateJobWithLock(Long id) {
        // Highest isolation level
    }
}
```

### Optimistic Locking

```java
@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;  // Optimistic locking

    // ... other fields
}
```


│   │   └── validator/
│   │       ├── CronExpressionValidator.java
│   │       └── JobPayloadValidator.java
│   ├── filter/
│   │   ├── CorrelationIdFilter.java
│   │   ├── RateLimitFilter.java
│   │   └── RequestLoggingFilter.java
│   └── interceptor/
│       └── AuthenticationInterceptor.java
│
├── service/                                # Service Layer
│   ├── job/
│   │   ├── JobService.java
│   │   ├── JobServiceImpl.java
│   │   └── JobValidationService.java
│   ├── execution/
│   │   ├── JobExecutionService.java
│   │   ├── JobExecutionServiceImpl.java
│   │   └── ExecutionContextService.java
│   ├── scheduling/
│   │   ├── JobSchedulingService.java
│   │   └── CronSchedulingService.java
│   └── notification/
│       └── NotificationService.java
│
├── domain/                                 # Domain Layer
│   ├── entity/
│   │   ├── Job.java
│   │   ├── JobExecution.java
│   │   ├── JobType.java
│   │   ├── JobDependency.java
│   │   ├── SchedulerNode.java
│   │   └── AuditLog.java
│   ├── valueobject/
│   │   ├── JobId.java
│   │   ├── CronExpression.java
│   │   ├── FencingToken.java
│   │   └── ExecutionResult.java
│   ├── event/
│   │   ├── JobCreatedEvent.java
│   │   ├── JobExecutedEvent.java
│   │   └── LeaderElectedEvent.java
│   ├── specification/
│   │   └── JobExecutionSpecification.java
│   └── enums/
│       ├── JobStatus.java
│       ├── ExecutionStatus.java
│       └── NodeRole.java
│
├── infrastructure/                         # Infrastructure Layer
│   ├── persistence/
│   │   ├── repository/
│   │   │   ├── JobRepository.java
│   │   │   ├── JobExecutionRepository.java
│   │   │   └── SchedulerNodeRepository.java
│   │   └── entity/
│   │       └── (JPA entity mappings if needed)
│   ├── cache/
│   │   ├── RedisCacheService.java
│   │   └── CacheKeyGenerator.java
│   ├── messaging/
│   │   └── EventPublisher.java
│   └── external/
│       └── WebhookClient.java
│
├── coordination/                           # Coordination Layer
│   ├── election/
│   │   ├── LeaderElectionService.java
│   │   ├── LeaderElectionServiceImpl.java
│   │   └── LeadershipListener.java
│   ├── locking/
│   │   ├── DistributedLockService.java
│   │   ├── RedlockManager.java
│   │   └── LockAcquisitionException.java
│   ├── heartbeat/
│   │   ├── HeartbeatService.java
│   │   └── HeartbeatScheduler.java
│   ├── fencing/
│   │   ├── FencingTokenProvider.java
│   │   └── FencingTokenValidator.java
│   └── cluster/
│       ├── ClusterStateService.java
│       └── NodeDiscoveryService.java
│
├── executor/                               # Job Execution
│   ├── JobExecutor.java
│   ├── VirtualThreadExecutor.java
│   ├── ExecutionContext.java
│   └── ExecutionMonitor.java
│
├── retry/                                  # Retry Logic
│   ├── RetryManager.java
│   ├── RetryPolicy.java
│   ├── BackoffStrategy.java
│   └── ExponentialBackoff.java
│
├── statemachine/                           # State Machine
│   ├── JobStateMachine.java
│   ├── StateTransitionValidator.java
│   └── StateChangeListener.java
│
├── config/                                 # Configuration
│   ├── DatabaseConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   ├── ExecutorConfig.java
│   ├── WebConfig.java
│   └── AsyncConfig.java
│
├── security/                               # Security
│   ├── authentication/
│   │   ├── JwtAuthenticationProvider.java
│   │   ├── JwtTokenService.java
│   │   └── UserDetailsServiceImpl.java
│   ├── authorization/
│   │   ├── RoleBasedAccessControl.java
│   │   └── PermissionEvaluator.java
│   └── audit/
│       ├── AuditService.java
│       └── AuditEventListener.java
│
└── common/                                 # Common/Shared
    ├── exception/
    │   ├── SchedulerException.java
    │   ├── JobNotFoundException.java
    │   ├── LeaderElectionException.java
    │   ├── LockAcquisitionException.java
    │   └── GlobalExceptionHandler.java
    ├── util/
    │   ├── DateTimeUtil.java
    │   ├── JsonUtil.java
    │   └── ValidationUtil.java
    ├── constant/
    │   ├── ApiConstants.java
    │   ├── CacheConstants.java
    │   └── SecurityConstants.java
    └── logging/
        ├── LoggingAspect.java
        └── MdcUtil.java
```


