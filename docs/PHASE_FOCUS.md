# Development Phase Focus Guide

**Last Updated**: 2026-03-07

---

## Current Development Strategy

We're focusing on **core distributed systems functionality** first, deferring observability features to Phase 4. This allows us to:

✅ Build a solid foundation  
✅ Iterate faster on core features  
✅ Reduce complexity during initial development  
✅ Add observability later without refactoring  

---

## Phase Priorities

### 🎯 **Phase 1: Core Infrastructure** (Current)

**Focus**: Database, entities, repositories, basic job execution

**What We're Building:**
- ✅ Project structure and Maven setup
- ✅ Database schema with Flyway
- ⏳ Core domain entities (Job, JobExecution, etc.)
- ⏳ JPA repositories
- ⏳ Basic configuration
- ⏳ Job service layer
- ⏳ REST API controllers
- ⏳ Single-node job executor

**What We're NOT Building Yet:**
- ❌ Custom metrics
- ❌ Prometheus integration
- ❌ Grafana dashboards
- ❌ Distributed tracing
- ❌ Custom health indicators

**Essential Monitoring Available:**
- ✅ `/actuator/health` - Basic health checks
- ✅ `/actuator/info` - Application info
- ✅ Logs - Structured logging with SLF4J

---

### 🎯 **Phase 2: Leader Election & Failover**

**Focus**: Redis-based coordination and automatic failover

**What We'll Build:**
- Redis configuration
- Leader election service
- Heartbeat mechanism
- Fencing token provider
- Cluster state manager

**What We're Still NOT Building:**
- ❌ Leadership metrics
- ❌ Failover dashboards
- ❌ Custom health indicators for leadership

**How to Monitor:**
- ✅ Logs - Leader election events
- ✅ Redis CLI - Check leader lock
- ✅ Database - Check scheduler_nodes table

---

### 🎯 **Phase 3: Distributed Locking & Job Execution**

**Focus**: Redlock algorithm and multi-node job execution

**What We'll Build:**
- Distributed lock service (Redlock)
- Idempotency service
- Retry manager with exponential backoff
- Job state machine
- Multi-node job execution

**What We're Still NOT Building:**
- ❌ Lock acquisition metrics
- ❌ Job execution metrics
- ❌ Retry metrics
- ❌ Performance dashboards

**How to Monitor:**
- ✅ Logs - Lock acquisition, job execution
- ✅ Database - job_executions table
- ✅ Redis CLI - Check job locks

---

### 🎯 **Phase 4: Observability & Monitoring** (Deferred)

**Focus**: Comprehensive metrics, tracing, and dashboards

**What We'll Build:**
- Prometheus metrics export
- Custom Micrometer metrics
- Grafana dashboards
- Custom health indicators
- Distributed tracing (OpenTelemetry)
- Alert rules

**Why Deferred:**
- Core functionality doesn't depend on it
- Can be added non-invasively (AOP, event listeners)
- Allows faster iteration on core features
- Reduces initial complexity

---

## How to Work Without Full Observability

### Debugging Leader Election

**Without Metrics:**
```bash
# Check Redis for leader lock
redis-cli GET scheduler:leader

# Check logs
tail -f logs/scheduler-dev.log | grep "Leader"

# Check database
mysql> SELECT * FROM scheduler_nodes WHERE status='LEADER';
```

**With Metrics (Phase 4):**
```bash
# Query Prometheus
curl http://localhost:9090/api/v1/query?query=scheduler_leader_elections_total

# View Grafana dashboard
open http://localhost:3000/d/leader-election
```

### Debugging Job Execution

**Without Metrics:**
```bash
# Check logs
tail -f logs/scheduler-dev.log | grep "Job execution"

# Check database
mysql> SELECT * FROM job_executions WHERE status='RUNNING';

# Check Redis for locks
redis-cli KEYS "job:lock:*"
```

**With Metrics (Phase 4):**
```bash
# Query job execution rate
curl http://localhost:9090/api/v1/query?query=rate(scheduler_jobs_executed_total[5m])

# View job execution dashboard
open http://localhost:3000/d/job-execution
```

### Debugging Distributed Locks

**Without Metrics:**
```bash
# Check Redis for locks
redis-cli KEYS "job:lock:*"
redis-cli GET "job:lock:12345"

# Check logs
tail -f logs/scheduler-dev.log | grep "Lock"

# Check database for stale locks
mysql> SELECT * FROM job_executions 
       WHERE status='RUNNING' 
       AND start_time < NOW() - INTERVAL 5 MINUTE;
```

**With Metrics (Phase 4):**
```bash
# Query lock acquisition latency
curl http://localhost:9090/api/v1/query?query=scheduler_lock_acquisition_duration_seconds

# View lock contention dashboard
open http://localhost:3000/d/distributed-locks
```

---

## Testing Strategy Per Phase

### Phase 1-3: Core Functionality

**Unit Tests:**
```java
@Test
void testJobCreation() {
    Job job = jobService.createJob(jobRequest);
    assertNotNull(job.getId());
    assertEquals(JobStatus.PENDING, job.getStatus());
}
```

**Integration Tests:**
```java
@SpringBootTest
@Testcontainers
class JobExecutionIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2");
    
    @Test
    void testJobExecution() {
        // Test without metrics
    }
}
```

**Manual Testing:**
```bash
# Start infrastructure
docker-compose up -d mysql redis

# Run application
mvn spring-boot:run

# Test API
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"test-job","cronExpression":"0 * * * *"}'

# Check health
curl http://localhost:8080/actuator/health
```

### Phase 4: Observability

**Metrics Tests:**
```java
@Test
void testJobExecutionMetrics() {
    jobExecutor.execute(job);
    
    Counter counter = meterRegistry.find("scheduler.jobs.executed").counter();
    assertEquals(1.0, counter.count());
}
```

**Integration Tests with Metrics:**
```java
@SpringBootTest
@AutoConfigureMetrics
class MetricsIntegrationTest {
    @Autowired
    MeterRegistry meterRegistry;
    
    @Test
    void testMetricsAreRecorded() {
        // Test with metrics enabled
    }
}
```

---

## Quick Reference: What's Available When

| Feature | Phase 1-3 | Phase 4 |
|---------|-----------|---------|
| **Health Checks** | ✅ Basic | ✅ Custom indicators |
| **Logging** | ✅ Structured | ✅ Structured + Tracing |
| **Metrics** | ❌ None | ✅ Prometheus |
| **Dashboards** | ❌ None | ✅ Grafana |
| **Tracing** | ❌ None | ✅ OpenTelemetry |
| **Alerts** | ❌ None | ✅ AlertManager |
| **Debugging** | ✅ Logs + DB | ✅ Logs + DB + Metrics |

---

## When to Enable Observability

Enable observability when:

1. ✅ **Core functionality is complete** (Phases 1-3 done)
2. ✅ **System is stable** (tests passing, no major bugs)
3. ✅ **Ready for production** (need monitoring and alerts)
4. ✅ **Performance tuning** (need metrics to identify bottlenecks)

Don't enable observability if:

1. ❌ **Still building core features** (adds complexity)
2. ❌ **Frequent refactoring** (metrics will change often)
3. ❌ **Just learning the system** (logs are sufficient)

---

## How to Enable Observability (Quick Guide)

When ready for Phase 4:

1. **Uncomment dependencies in `pom.xml`**
2. **Enable metrics in `application.yml`**
3. **Uncomment Prometheus/Grafana in `docker-compose.yml`**
4. **Implement custom metrics collectors**
5. **Create Grafana dashboards**
6. **Set up alert rules**

See [OBSERVABILITY_STRATEGY.md](./OBSERVABILITY_STRATEGY.md) for detailed instructions.

---

## Summary

**Current Focus (Phases 1-3):**
- 🎯 Database schema and entities
- 🎯 Leader election and failover
- 🎯 Distributed locking
- 🎯 Job execution and retry logic

**Deferred to Phase 4:**
- ⏸️ Prometheus metrics
- ⏸️ Grafana dashboards
- ⏸️ Custom health indicators
- ⏸️ Distributed tracing

**Always Available:**
- ✅ Basic health checks (`/actuator/health`)
- ✅ Structured logging
- ✅ Database queries for debugging

---

**Remember**: Observability is **additive**, not **foundational**. We can build a fully functional distributed job scheduler without it, then add comprehensive monitoring later without refactoring core logic.

---

**Last Updated**: 2026-03-07  
**Next Review**: Before starting Phase 4

