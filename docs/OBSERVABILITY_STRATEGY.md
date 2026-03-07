# Observability Strategy - Deferred to Phase 4

**Decision Date**: 2026-03-07  
**Status**: Observability features deferred to Phase 4  
**Rationale**: Focus on core distributed systems functionality first

---

## Overview

This document explains the strategy for deferring observability features (Prometheus, Grafana, custom metrics, distributed tracing) to Phase 4, allowing us to focus on core distributed systems functionality in Phases 1-3.

---

## What's Been Deferred

### Deferred to Phase 4: Observability & Monitoring

| Feature | Status | Will Be Added In |
|---------|--------|------------------|
| Prometheus metrics export | ❌ Disabled | Phase 4 |
| Grafana dashboards | ❌ Disabled | Phase 4 |
| Custom Micrometer metrics | ❌ Disabled | Phase 4 |
| Distributed tracing (OpenTelemetry) | ❌ Not added | Phase 4 |
| Custom health indicators | ❌ Not added | Phase 4 |
| Detailed metrics endpoints | ❌ Disabled | Phase 4 |

### Kept for Essential Monitoring

| Feature | Status | Purpose |
|---------|--------|---------|
| Spring Boot Actuator | ✅ Enabled (minimal) | Basic health checks |
| `/actuator/health` endpoint | ✅ Enabled | Load balancer health probes |
| `/actuator/info` endpoint | ✅ Enabled | Application information |
| Liveness/Readiness probes | ✅ Enabled | Kubernetes health checks |
| Basic logging | ✅ Enabled | Debugging and troubleshooting |

---

## Changes Made

### 1. Maven Dependencies (`pom.xml`)

**Commented Out:**
```xml
<!-- Metrics - OPTIONAL (Phase 4: Observability) -->
<!-- Uncomment when implementing observability features -->
<!--
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
-->
```

**Impact**: 
- Application still compiles and runs
- No Prometheus metrics exported
- Micrometer auto-configuration skipped
- Smaller JAR size

### 2. Application Configuration (`application.yml`)

**Disabled:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Removed: metrics, prometheus, env, loggers
  
  # Metrics disabled until Phase 4
  # metrics:
  #   export:
  #     prometheus:
  #       enabled: true
```

**Impact**:
- Only essential endpoints exposed
- No metrics collection overhead
- Reduced attack surface (fewer endpoints)

### 3. Docker Compose (`docker-compose.yml`)

**Commented Out:**
```yaml
# Prometheus - DISABLED (Phase 4: Observability)
# prometheus:
#   image: prom/prometheus:latest
#   ...

# Grafana - DISABLED (Phase 4: Observability)
# grafana:
#   image: grafana/grafana:latest
#   ...
```

**Impact**:
- Faster startup (fewer containers)
- Reduced resource usage
- Simpler local development environment

---

## What Still Works

### ✅ Core Functionality (Unaffected)

All core distributed systems features work perfectly without observability:

1. **Leader Election**
   - Redis-based coordination
   - Heartbeat mechanism
   - Automatic failover
   - Fencing tokens

2. **Distributed Locking**
   - Redlock algorithm
   - Lock acquisition/release
   - TTL-based expiry
   - Deadlock prevention

3. **Job Execution**
   - Job scheduling
   - State machine transitions
   - Retry logic with exponential backoff
   - Dead letter queue

4. **Database Operations**
   - JPA/Hibernate persistence
   - Flyway migrations
   - Transaction management
   - Optimistic locking

5. **REST API**
   - Job CRUD operations
   - Cluster status endpoints
   - Error handling

### ✅ Essential Monitoring (Still Available)

Even without full observability, you can still monitor the system:

1. **Health Checks**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Response:
   ```json
   {
     "status": "UP"
   }
   ```

2. **Application Info**
   ```bash
   curl http://localhost:8080/actuator/info
   ```

3. **Logs**
   - Structured logging with SLF4J
   - Log files in `logs/` directory
   - Console output for debugging

4. **Database Queries**
   - Direct MySQL queries to check job status
   - Execution history in `job_executions` table

---

## How to Add Observability Later (Phase 4)

When you're ready to implement observability, follow these steps:

### Step 1: Uncomment Dependencies

In `pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Step 2: Enable Metrics in Configuration

In `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### Step 3: Enable Prometheus & Grafana

In `docker-compose.yml`:
```yaml
# Uncomment the prometheus and grafana services
```

### Step 4: Implement Custom Metrics

Create custom metrics collectors:
```java
@Component
public class JobMetricsCollector {
    private final MeterRegistry meterRegistry;
    
    public void recordJobExecution(String jobType, String status, long duration) {
        Counter.builder("scheduler.jobs.executed")
            .tag("job_type", jobType)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
        
        Timer.builder("scheduler.job.duration")
            .tag("job_type", jobType)
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### Step 5: Create Custom Health Indicators

```java
@Component
public class LeadershipHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        boolean isLeader = leaderElectionService.isLeader();
        return isLeader 
            ? Health.up().withDetail("role", "LEADER").build()
            : Health.up().withDetail("role", "FOLLOWER").build();
    }
}
```

---

## Architecture Benefits

### Clean Separation of Concerns

```
┌─────────────────────────────────────────┐
│         Application Core                │
│  (Leader Election, Locking, Jobs)       │
│         NO DEPENDENCIES ON               │
│      OBSERVABILITY COMPONENTS            │
└─────────────────────────────────────────┘
                    │
                    │ Observes (one-way)
                    ▼
┌─────────────────────────────────────────┐
│      Observability Layer (Phase 4)      │
│  (Metrics, Tracing, Custom Indicators)  │
│         OPTIONAL & ADDITIVE              │
└─────────────────────────────────────────┘
```

**Key Principles:**
1. **Core logic never calls observability code**
2. **Observability observes core logic (one-way dependency)**
3. **Metrics are emitted, not queried**
4. **Health indicators read state, don't modify it**

### Future-Proof Design

When adding observability in Phase 4, you'll use:

1. **Aspect-Oriented Programming (AOP)**
   ```java
   @Aspect
   @Component
   public class MetricsAspect {
       @Around("@annotation(Timed)")
       public Object recordMetrics(ProceedingJoinPoint joinPoint) {
           // Record metrics without modifying core logic
       }
   }
   ```

2. **Event Listeners**
   ```java
   @EventListener
   public void onJobExecuted(JobExecutedEvent event) {
       metricsCollector.recordJobExecution(event);
   }
   ```

3. **Micrometer Auto-Configuration**
   - Spring Boot automatically configures metrics
   - No code changes needed in core services

---

## Testing Without Observability

### Unit Tests
- No changes needed
- Mock any metrics collectors (if added later)

### Integration Tests
- Testcontainers for MySQL and Redis
- No Prometheus/Grafana needed
- Health checks still work

### Manual Testing
```bash
# Start infrastructure only
docker-compose up -d mysql redis

# Run application
mvn spring-boot:run

# Test health
curl http://localhost:8080/actuator/health

# Test API
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"test-job","cronExpression":"0 * * * *"}'
```

---

## Comparison: With vs Without Observability

| Aspect | Without Observability (Phase 1-3) | With Observability (Phase 4) |
|--------|-----------------------------------|------------------------------|
| **Startup Time** | ~5 seconds | ~7 seconds |
| **Memory Usage** | ~500MB | ~700MB |
| **Docker Containers** | 5 (3 schedulers, MySQL, Redis) | 7 (+ Prometheus, Grafana) |
| **Exposed Endpoints** | 2 (`/health`, `/info`) | 6+ (+ `/metrics`, `/prometheus`) |
| **Dependencies** | 12 | 14 |
| **JAR Size** | ~50MB | ~55MB |
| **Debugging** | Logs + Database queries | Logs + Metrics + Dashboards |

---

## Recommended Development Flow

### Phase 1-3: Core Development
```bash
# Minimal setup
docker-compose up -d mysql redis
mvn spring-boot:run

# Focus on:
# - Database schema
# - Domain entities
# - Leader election
# - Distributed locking
# - Job execution
```

### Phase 4: Add Observability
```bash
# Full setup
docker-compose up -d  # Includes Prometheus & Grafana
mvn spring-boot:run

# Implement:
# - Custom metrics
# - Grafana dashboards
# - Alert rules
# - Distributed tracing
```

---

## Conclusion

**Deferring observability to Phase 4 allows us to:**

✅ Focus on core distributed systems concepts  
✅ Faster development iterations  
✅ Simpler local development environment  
✅ Reduced cognitive load  
✅ Cleaner architecture (separation of concerns)  

**We still maintain:**

✅ Essential health checks  
✅ Basic logging  
✅ Ability to add observability later without refactoring  
✅ Production-ready architecture  

**When we add observability in Phase 4, it will be:**

✅ Non-invasive (AOP, event listeners)  
✅ Optional (can be disabled)  
✅ Comprehensive (metrics, tracing, dashboards)  
✅ Production-grade (Prometheus, Grafana, alerts)  

---

**Last Updated**: 2026-03-07  
**Next Review**: Before starting Phase 4

