# Week 2: Coordination Layer Implementation

**Date**: 2026-03-07  
**Status**: ✅ **COMPLETE**

---

## Overview

Implemented the **Coordination Layer** - the core distributed systems component of the job scheduler. This layer provides Redis-based leader election, distributed locking, fencing tokens, and heartbeat mechanisms.

---

## Components Implemented

### 1. **SchedulerProperties** ✅
**File**: `src/main/java/com/scheduler/config/SchedulerProperties.java`

**Purpose**: Type-safe configuration binding for scheduler settings

**Features**:
- `@ConfigurationProperties` for YAML binding
- Validation with `@Min` and `@NotBlank`
- Nested configuration classes for:
  - Node identification
  - Leader election settings
  - Job execution settings
  - Distributed lock settings

**Interview Talking Point**:
> "I use @ConfigurationProperties for type-safe configuration. This provides validation, IDE autocomplete, and makes it easy to override settings per environment."

---

### 2. **RedisConfig** ✅
**File**: `src/main/java/com/scheduler/config/RedisConfig.java`

**Purpose**: Redisson client configuration for distributed coordination

**Features**:
- Single-server Redis configuration
- Connection pooling (64 connections, 10 minimum idle)
- Automatic retry (3 attempts, 1.5s interval)
- Ping interval for connection health (30s)
- RedisTemplate with String serializers

**Interview Talking Points**:
> "I use Redisson over Jedis because it provides high-level abstractions for distributed patterns like Redlock, semaphores, and rate limiters."
>
> "The single-server configuration is simple but can be upgraded to Redis Sentinel for automatic failover or Redis Cluster for horizontal scaling."

---

### 3. **RedisCoordinationService** ✅
**File**: `src/main/java/com/scheduler/coordination/RedisCoordinationService.java`

**Purpose**: Redis implementation of the CoordinationService interface

**Features**:
- **Leader Election**:
  - TTL-based locks for automatic failover
  - `tryAcquireLeadership()` - atomic lock acquisition
  - `renewLeadership()` - heartbeat renewal
  - `releaseLeadership()` - graceful shutdown
- **Distributed Locking**:
  - Redlock algorithm via Redisson
  - `tryAcquireLock()` - acquire lock with TTL
  - `renewLock()` - extend lock for long-running jobs
  - `releaseLock()` - explicit lock release
  - `isLockHeld()` - check lock status
  - `getLockTTL()` - get remaining lock time

**Interview Talking Points**:
> "I chose Redis (AP system) over Zookeeper (CP system) because availability is more important than strong consistency for job scheduling."
>
> "Fencing tokens provide safety even if Redis has split-brain scenarios."
>
> "TTL-based leases ensure automatic failover if the leader crashes."

---

### 4. **LeaderElectionService** ✅
**File**: `src/main/java/com/scheduler/coordination/LeaderElectionService.java`

**Purpose**: Manages distributed leader election with automatic failover

**Features**:
- **Automatic Leader Election**:
  - Attempts to acquire leadership on startup
  - Periodic heartbeat renewal (every TTL/3)
  - Automatic failover when leader crashes
- **Epoch Management**:
  - Increments epoch on each leader election
  - Tracks leadership in database for observability
- **State Transitions**:
  - `transitionToLeader()` - promote to leader, increment epoch
  - `transitionToFollower()` - demote to follower
  - `releaseLeadership()` - graceful shutdown
- **Lifecycle Management**:
  - `@PostConstruct` - initialize and start election
  - `@PreDestroy` - graceful shutdown and cleanup

**Interview Talking Points**:
> "I use TTL-based leases for leader election. If the leader crashes, the lease expires automatically and followers can compete for leadership."
>
> "Heartbeat interval is TTL/3 to allow for 2 missed heartbeats before failover. This balances fast failover with tolerance for transient network issues."
>
> "Epoch numbers provide fencing tokens to prevent zombie leaders from corrupting state after network partitions."

---

### 5. **FencingTokenProvider** ✅
**File**: `src/main/java/com/scheduler/coordination/FencingTokenProvider.java`

**Purpose**: Provides fencing tokens to prevent split-brain scenarios

**Features**:
- **Token Generation**:
  - `getCurrentFencingToken()` - get current leader's epoch
  - `getMyFencingToken()` - get this node's epoch if leader
- **Token Validation**:
  - `isTokenValid()` - check if token matches current epoch
  - `isTokenStale()` - check if token is from previous epoch
- **Token Formatting**:
  - Format: `epoch{N}-node{ID}`

**Interview Talking Points**:
> "Fencing tokens solve the split-brain problem where two nodes think they're leader."
>
> "Each leader has a unique epoch number that's monotonically increasing. The database validates that writes come from the current epoch."
>
> "Even if Redis has a split-brain, the database rejects writes from stale epochs."

**Example Scenario**:
1. Node A is leader with epoch 5
2. Network partition occurs
3. Node B becomes leader with epoch 6
4. Node A recovers and tries to write with epoch 5
5. Database rejects the write because epoch 6 > epoch 5

---

### 6. **DistributedLockService** ✅
**File**: `src/main/java/com/scheduler/coordination/DistributedLockService.java`

**Purpose**: High-level distributed locking with automatic management

**Features**:
- **Functional API**:
  - `executeWithLock(lockKey, Supplier<T>)` - execute task with lock
  - `executeWithLock(lockKey, Runnable)` - execute void task with lock
  - Automatic lock release via try-finally
- **Manual Lock Management**:
  - `tryAcquire()` - acquire lock
  - `release()` - release lock
  - `renew()` - extend lock TTL
  - `isHeld()` - check lock status
- **Helper Methods**:
  - `createJobLockKey(jobId)` - format: `job:{jobId}`
  - `createExecutionLockKey(executionId)` - format: `execution:{executionId}`

**Interview Talking Points**:
> "I use the Redlock algorithm for distributed mutual exclusion. Locks have TTL to prevent deadlocks if a node crashes."
>
> "I use a functional approach with Supplier to ensure the lock is always released, even if the task throws an exception."
>
> "For long-running jobs, I implement automatic lock renewal to prevent premature expiry."

---

### 7. **HeartbeatService** ✅
**File**: `src/main/java/com/scheduler/coordination/HeartbeatService.java`

**Purpose**: Node heartbeat mechanism for failure detection

**Features**:
- **Heartbeat Sending**:
  - Periodic heartbeats at configured interval
  - Updates `last_heartbeat_at` in database
  - Tracks node health status
- **Stale Node Detection**:
  - Detects nodes with stale heartbeats (>3x interval)
  - Marks stale nodes as unhealthy
  - Demotes stale leaders to followers
- **Lifecycle Management**:
  - Scheduled executor for periodic tasks
  - Graceful shutdown

**Interview Talking Points**:
> "Heartbeats are critical for failure detection. I send heartbeats at 1/3 of the TTL interval to allow for 2 missed heartbeats."
>
> "I use both Redis TTL (for leader election) and database heartbeats (for observability). Redis is the source of truth, but database provides historical records."

---

## Architecture Decisions

### **Redis vs Zookeeper**
✅ **Decision**: Use Redis (AP system) for coordination

**Rationale**:
- Availability > Strong Consistency for job scheduling
- Lower latency than Zookeeper
- Simpler to operate and deploy
- Built-in TTL support for automatic lease expiry
- Fencing tokens compensate for weaker consistency

### **TTL-Based Leases**
✅ **Decision**: Use TTL-based locks for leader election

**Rationale**:
- Automatic failover if leader crashes
- No need for explicit failure detection
- Heartbeat renewal at TTL/3 allows for 2 missed heartbeats
- Balances fast failover with tolerance for transient issues

### **Epoch-Based Fencing Tokens**
✅ **Decision**: Use monotonically increasing epoch numbers

**Rationale**:
- Prevents split-brain scenarios
- Database validates writes against current epoch
- Zombie leaders cannot corrupt state
- Simple and effective

---

## Configuration

### application.yml
```yaml
scheduler:
  node:
    id: ${HOSTNAME:localhost}-${SERVER_PORT:8080}
  
  leader-election:
    enabled: true
    lock-key: "scheduler:leader"
    lock-ttl-seconds: 10
    heartbeat-interval-seconds: 3
  
  distributed-lock:
    lock-ttl-seconds: 60
    wait-time-seconds: 5
    lease-time-seconds: 60
```

---

## Build Status

```bash
mvn clean compile
# Result: BUILD SUCCESS ✅
# Time: 11.541 seconds
# Files compiled: 18 source files
```

---

## Post-Implementation Fix: Watchdog Mechanism

### Issue Identified
After implementation, a bug was discovered in the `renewLeadership()` method: it didn't actually extend the Redis lock TTL, causing unnecessary leader churn every ~10 seconds.

### Fix Applied
✅ **Enabled Redisson's Watchdog Mechanism**
- Changed `tryAcquireLeadership()` to use `leaseTime = -1` (enables watchdog)
- Watchdog automatically renews lock every 10 seconds (default: 30s TTL / 3)
- `renewLeadership()` now just verifies lock is held (watchdog handles renewal)
- Eliminates unnecessary leader churn
- Leader holds lock indefinitely while healthy

**Details**: See `docs/LEADER_ELECTION_WATCHDOG_FIX.md`

---

## Next Steps

**Week 3: Execution Layer** (Not started)
- Implement `JobService` for job management
- Implement `JobExecutionService` for execution tracking
- Implement `JobExecutor` with virtual threads
- Implement `RetryManager` for failed jobs

---

**Week 2 Coordination Layer implementation complete! 🎉**

