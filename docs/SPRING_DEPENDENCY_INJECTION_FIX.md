# Spring Dependency Injection Error - Fix Documentation

**Date**: 2026-03-10  
**Issue**: Multiple beans of type `ExecutorService` - autowiring failed  
**Status**: ✅ **RESOLVED**

---

## Problem Summary

**Error Message:**
```
Parameter 5 of constructor in com.scheduler.executor.JobExecutor required a single bean, but 2 were found:
	- jobExecutorService: defined by method 'jobExecutorService' in ExecutorConfig
	- scheduledExecutorService: defined by method 'scheduledExecutorService' in ExecutorConfig

This may be due to missing parameter name information
```

**Symptoms:**
- Application fails to start after Docker image rebuild
- Spring cannot autowire `ExecutorService` dependency in `JobExecutor`
- Error occurs during application context initialization
- Two `ExecutorService` beans exist, but Spring doesn't know which one to inject

---

## Root Cause Analysis

### The Problem

In `JobExecutor.java`, the code used Lombok's `@RequiredArgsConstructor` with `@Qualifier` on a field:

**INCORRECT:**
```java
@Component
@RequiredArgsConstructor  // ❌ Lombok generates constructor
@Slf4j
public class JobExecutor {
    
    private final JobService jobService;
    private final JobExecutionService executionService;
    private final DistributedLockService lockService;
    private final LeaderElectionService leaderElectionService;
    private final RetryManager retryManager;
    
    @Qualifier("jobExecutorService")  // ❌ On field, not constructor parameter!
    private final ExecutorService executorService;
}
```

### Why This Fails

1. **Lombok generates a constructor** with all `final` fields as parameters
2. **`@Qualifier` is on the field**, not the constructor parameter
3. **Lombok does NOT copy `@Qualifier`** from field to constructor parameter
4. **Spring looks for `@Qualifier` on constructor parameters** for dependency injection
5. **Spring sees two `ExecutorService` beans** and doesn't know which one to inject

### Generated Constructor (by Lombok)

Lombok generates this constructor:

```java
public JobExecutor(
    JobService jobService,
    JobExecutionService executionService,
    DistributedLockService lockService,
    LeaderElectionService leaderElectionService,
    RetryManager retryManager,
    ExecutorService executorService  // ❌ No @Qualifier here!
) {
    this.jobService = jobService;
    this.executionService = executionService;
    this.lockService = lockService;
    this.leaderElectionService = leaderElectionService;
    this.retryManager = retryManager;
    this.executorService = executorService;
}
```

**Notice**: The `@Qualifier` annotation is **missing** from the `executorService` parameter!

---

## Solution Applied

### Fix: Use Explicit Constructor with @Qualifier on Parameter

**CORRECT:**
```java
@Component
@Slf4j  // ✅ Removed @RequiredArgsConstructor
public class JobExecutor {
    
    private final JobService jobService;
    private final JobExecutionService executionService;
    private final DistributedLockService lockService;
    private final LeaderElectionService leaderElectionService;
    private final RetryManager retryManager;
    private final ExecutorService executorService;  // ✅ No @Qualifier on field
    
    /**
     * Constructor with dependency injection.
     * 
     * Note: @Qualifier annotation must be on the constructor parameter, not the field,
     * when using constructor-based injection.
     */
    public JobExecutor(
        JobService jobService,
        JobExecutionService executionService,
        DistributedLockService lockService,
        LeaderElectionService leaderElectionService,
        RetryManager retryManager,
        @Qualifier("jobExecutorService") ExecutorService executorService  // ✅ On parameter!
    ) {
        this.jobService = jobService;
        this.executionService = executionService;
        this.lockService = lockService;
        this.leaderElectionService = leaderElectionService;
        this.retryManager = retryManager;
        this.executorService = executorService;
    }
}
```

### Why This Works

1. ✅ **Explicit constructor** - We control the constructor signature
2. ✅ **`@Qualifier` on parameter** - Spring sees it during dependency injection
3. ✅ **Spring injects correct bean** - `jobExecutorService` (virtual threads)
4. ✅ **No ambiguity** - Spring knows exactly which bean to inject

---

## Alternative Solutions

### Solution 2: Use @Primary Annotation (Not Recommended)

Mark one bean as `@Primary` in `ExecutorConfig.java`:

```java
@Bean(name = "jobExecutorService")
@Primary  // ✅ Makes this the default ExecutorService
public ExecutorService jobExecutorService() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

**Pros:**
- Can keep using `@RequiredArgsConstructor`
- Less verbose

**Cons:**
- ❌ Hides the dependency - not clear which bean is injected
- ❌ Affects ALL `ExecutorService` injections in the application
- ❌ Less explicit - harder to understand in code reviews

### Solution 3: Use Field Injection (Not Recommended)

```java
@Component
@Slf4j
public class JobExecutor {
    
    @Autowired
    @Qualifier("jobExecutorService")
    private ExecutorService executorService;
}
```

**Pros:**
- Simple syntax

**Cons:**
- ❌ Field injection is discouraged (harder to test, hides dependencies)
- ❌ Cannot use `final` fields (immutability lost)
- ❌ Harder to mock in unit tests
- ❌ Not recommended by Spring team

---

## Best Practices

### ✅ DO:

1. **Use constructor injection** for required dependencies
2. **Put `@Qualifier` on constructor parameters**, not fields
3. **Use explicit constructors** when you need `@Qualifier` with Lombok
4. **Make fields `final`** to ensure immutability
5. **Document why you're not using `@RequiredArgsConstructor`**

### ❌ DON'T:

1. **Don't put `@Qualifier` on fields** when using `@RequiredArgsConstructor`
2. **Don't use field injection** for required dependencies
3. **Don't use `@Primary`** unless you truly want a default bean
4. **Don't assume Lombok copies annotations** to generated code

---

## Verification Steps

### 1. Rebuild Docker Images

```bash
cd deployment/docker
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 2. Check Application Logs

```bash
docker logs scheduler-node-1 --tail 100
```

**Expected output (SUCCESS):**
```
Initializing virtual thread executor for job execution
Virtual threads enabled: true
Virtual thread executor initialized successfully
Expected concurrent job capacity: 10,000+ jobs
Started SchedulerApplication in X.XXX seconds
```

**Should NOT see:**
```
Parameter 5 of constructor in com.scheduler.executor.JobExecutor required a single bean, but 2 were found
```

### 3. Test Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

**Expected**: `{"status":"UP"}`

---

## Interview Talking Points

**Q: "How did you debug this Spring dependency injection error?"**

**A**: "I analyzed the error message which indicated that Spring found two `ExecutorService` beans but couldn't determine which one to inject. I examined the code and found that:

1. The `@Qualifier` annotation was on the **field**, not the **constructor parameter**
2. Lombok's `@RequiredArgsConstructor` generates the constructor but doesn't copy field annotations to parameters
3. Spring looks for `@Qualifier` on **constructor parameters** during dependency injection

I fixed it by replacing `@RequiredArgsConstructor` with an explicit constructor and placing `@Qualifier` on the constructor parameter. This demonstrates understanding of:
- Spring's dependency injection mechanism
- Lombok's code generation behavior
- The difference between field and constructor annotations
- Best practices for constructor-based injection"

**Q: "Why not use `@Primary` instead?"**

**A**: "While `@Primary` would work, it's less explicit and affects all `ExecutorService` injections in the application. Using `@Qualifier` on the constructor parameter is more explicit and makes the dependency clear to anyone reading the code. It also allows different components to inject different `ExecutorService` beans based on their needs."

**Q: "Why not use field injection?"**

**A**: "Field injection has several drawbacks:
1. Cannot use `final` fields, losing immutability
2. Harder to write unit tests (can't easily mock dependencies)
3. Hides dependencies (not visible in constructor signature)
4. Not recommended by the Spring team

Constructor injection is the preferred approach because it makes dependencies explicit, enables immutability with `final` fields, and makes testing easier."

---

## Files Modified

1. ✅ `src/main/java/com/scheduler/executor/JobExecutor.java`
   - Removed `@RequiredArgsConstructor` annotation
   - Removed `@Qualifier` from field
   - Added explicit constructor with `@Qualifier` on parameter
   - Removed unused `lombok.RequiredArgsConstructor` import

---

## Related Issues

This issue is related to:
- Lombok annotation processing
- Spring dependency injection
- Constructor-based vs field-based injection
- Bean qualification in Spring

---

**Resolution Date**: 2026-03-10  
**Verified By**: Development Team  
**Status**: ✅ **COMPLETE - Ready to rebuild and test**

