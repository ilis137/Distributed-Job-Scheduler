# Leader Election Issue - Diagnosis and Fix

**Date**: 2026-03-12  
**Issue**: All nodes are FOLLOWER, no LEADER elected  
**Status**: ⚠️ CRITICAL BUG IDENTIFIED

---

## 🔍 Root Cause Analysis

### **The Problem: Redisson Watchdog vs. Manual TTL**

The leader election is failing due to a **fundamental mismatch** between how the code is designed and how Redisson's lock mechanism works.

#### **What the Code Expects** (Lines 48-74 in `RedisCoordinationService.java`)

```java
// tryAcquireLeadership() uses -1 for leaseTime
boolean acquired = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
```

**What `-1` means in Redisson**:
- Enables **Redisson's Watchdog** mechanism
- Watchdog automatically renews the lock every 10 seconds (default: `lockWatchdogTimeout / 3`)
- Lock **never expires** as long as the JVM is alive
- **Ignores the `ttl` parameter** passed to the method!

#### **What the Code Actually Does** (Lines 76-98)

```java
// renewLeadership() just checks if lock is still held
if (lock.isHeldByCurrentThread()) {
    return true;  // Watchdog is renewing automatically
}
```

**The Issue**:
1. `tryAcquireLeadership()` is called with a `ttl` parameter (10 seconds from config)
2. **But the TTL is completely ignored** because `-1` enables watchdog
3. The lock is held **indefinitely** by the first node that acquires it
4. When that node shuts down gracefully, it releases the lock
5. **But if multiple nodes start simultaneously**, they all try to acquire the lock
6. Only ONE succeeds, but the others **never try again** because:
   - The scheduled task runs every 3 seconds
   - It checks `if (isLeader.get())` first
   - If not leader, it tries to acquire
   - **But the lock is held indefinitely by the first node**
   - So all other nodes remain FOLLOWER forever

---

## 🐛 The Critical Bug

### **Scenario: All Nodes Start at the Same Time**

1. **Node 1** starts, tries to acquire leadership → **SUCCESS** (becomes LEADER)
2. **Node 2** starts 100ms later, tries to acquire → **FAILS** (Node 1 holds lock)
3. **Node 3** starts 200ms later, tries to acquire → **FAILS** (Node 1 holds lock)

**Expected Behavior**:
- Node 1 should be LEADER in database
- Nodes 2 and 3 should be FOLLOWER in database

**Actual Behavior** (The Bug):
- Node 1 acquires the Redis lock successfully
- Node 1 calls `transitionToLeader()` → Updates database to LEADER
- **BUT**: If Node 1's database transaction hasn't committed yet...
- Nodes 2 and 3 also call `transitionToLeader()` (race condition!)
- **All nodes think they're leader** for a brief moment
- Then they all call `renewLeadership()`
- Only Node 1's `lock.isHeldByCurrentThread()` returns true
- Nodes 2 and 3 get `false` → call `transitionToFollower()`
- **Result**: All nodes end up as FOLLOWER in the database!

---

## 🔬 Why This Happens

### **Race Condition in `attemptLeaderElection()`**

```java
// LeaderElectionService.java, lines 135-163
private void attemptLeaderElection() {
    if (isLeader.get()) {
        // Renew leadership
        boolean renewed = coordinationService.renewLeadership(nodeId);
        if (renewed) {
            updateHeartbeat();  // ✅ Still leader
        } else {
            transitionToFollower();  // ❌ Lost leadership
        }
    } else {
        // Try to become leader
        boolean acquired = coordinationService.tryAcquireLeadership(nodeId, ttl);
        if (acquired) {
            transitionToLeader();  // ⚠️ RACE CONDITION HERE!
        }
    }
}
```

**The Problem**:
1. `tryAcquireLeadership()` acquires the Redis lock (thread-safe)
2. `transitionToLeader()` updates the database (NOT atomic with Redis lock!)
3. Between steps 1 and 2, another node might also acquire the lock (if using wrong TTL)
4. Or worse: the lock is acquired, but the database update fails/delays
5. On the next heartbeat cycle, `renewLeadership()` checks `isHeldByCurrentThread()`
6. **This is thread-local!** Each node's thread will return `false` if it didn't acquire the lock
7. So all nodes that didn't acquire the lock will call `transitionToFollower()`
8. **But the node that DID acquire the lock might have already transitioned to FOLLOWER** if its database update was slow!

---

## 🔧 The Fix

### **Option 1: Use Explicit TTL (Recommended)**

Change `RedisCoordinationService.tryAcquireLeadership()` to use the actual TTL:

```java
@Override
public boolean tryAcquireLeadership(String nodeId, Duration ttl) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // Use explicit TTL instead of watchdog (-1)
        // This ensures the lock expires if heartbeats fail
        boolean acquired = lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);

        if (acquired) {
            log.info("Node {} acquired leadership with TTL {}ms", nodeId, ttl.toMillis());
            return true;
        } else {
            log.debug("Node {} failed to acquire leadership", nodeId);
            return false;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Interrupted while trying to acquire leadership", e);
        return false;
    }
}
```

**And update `renewLeadership()` to actually renew the TTL**:

```java
@Override
public boolean renewLeadership(String nodeId) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        if (lock.isHeldByCurrentThread()) {
            // Renew the lock by extending its TTL
            // Redisson doesn't have a direct "extend TTL" method
            // So we need to use forceUnlock() and re-acquire
            // OR use a different approach with Redis SET commands
            
            // For now, just verify the lock is still held
            // The watchdog will handle renewal
            log.debug("Node {} leadership verified", nodeId);
            return true;
        } else {
            log.warn("Node {} lost leadership", nodeId);
            return false;
        }
    } catch (Exception e) {
        log.error("Error verifying leadership", e);
        return false;
    }
}
```

---

## 📊 Diagnostic Commands

### **1. Check Redis Leader Lock**

```bash
# Connect to Redis container
docker exec -it redis redis-cli

# Check if leader lock exists
EXISTS scheduler:leader:election

# Get lock details (if using Redis strings instead of Redisson locks)
GET scheduler:leader

# Check TTL
TTL scheduler:leader:election

# List all scheduler keys
KEYS scheduler:*
```

**Expected Output**:
- `EXISTS scheduler:leader:election` → `1` (lock exists)
- `TTL scheduler:leader:election` → `-1` (never expires with watchdog) or `7` (seconds remaining)

---

### **2. Check Database State**

```sql
-- Connect to MySQL
docker exec -it mysql mysql -u scheduler -pscheduler123 scheduler_db

-- Check all nodes
SELECT node_id, role, epoch, healthy, last_heartbeat 
FROM scheduler_nodes 
ORDER BY last_heartbeat DESC;

-- Expected: ONE node with role='LEADER', others with role='FOLLOWER'
```

---

### **3. Check Application Logs**

```bash
# Check Node 1 logs
docker logs scheduler-node-1 --tail 100 | grep -i "leader"

# Look for these messages:
# ✅ "Node scheduler-node-1 acquired leadership"
# ✅ "Node scheduler-node-1 transitioning to LEADER"
# ✅ "Node scheduler-node-1 is now LEADER with epoch 1"
# ❌ "Failed to renew leadership - transitioning to follower"
# ❌ "Node scheduler-node-1 transitioning to FOLLOWER"
```

---

## 🚨 Immediate Workaround

If you need to force a leader election right now:

### **Option A: Restart All Nodes**

```bash
docker-compose restart scheduler-node-1 scheduler-node-2 scheduler-node-3
```

### **Option B: Clear Redis Lock Manually**

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Delete the leader lock
DEL scheduler:leader:election

# Verify it's gone
EXISTS scheduler:leader:election
# Should return 0
```

Then wait 3 seconds for the next election cycle.

---

## 📝 Next Steps

1. **Apply the fix** to `RedisCoordinationService.java`
2. **Add integration tests** for leader election
3. **Add logging** to track lock acquisition/renewal
4. **Monitor** Redis and database for consistency

---

## 🛠️ Detailed Fix Implementation

### **Step 1: Update `RedisCoordinationService.java`**

Replace the `tryAcquireLeadership()` and `renewLeadership()` methods:

```java
@Override
public boolean tryAcquireLeadership(String nodeId, Duration ttl) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // Use explicit TTL (lease time) instead of watchdog
        // waitTime = 0: don't wait if lock is already held
        // leaseTime = ttl: lock expires after TTL if not renewed
        boolean acquired = lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);

        if (acquired) {
            log.info("Node {} acquired leadership with TTL {}s", nodeId, ttl.toSeconds());
            return true;
        } else {
            log.debug("Node {} failed to acquire leadership - lock held by another node", nodeId);
            return false;
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Interrupted while trying to acquire leadership for node {}", nodeId, e);
        return false;
    } catch (Exception e) {
        log.error("Error acquiring leadership for node {}", nodeId, e);
        return false;
    }
}

@Override
public boolean renewLeadership(String nodeId) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // Check if this thread still holds the lock
        if (!lock.isHeldByCurrentThread()) {
            log.warn("Node {} lost leadership - lock no longer held by this thread", nodeId);
            return false;
        }

        // Get remaining TTL
        long remainingTimeMs = lock.remainTimeToLive();

        if (remainingTimeMs <= 0) {
            log.warn("Node {} lost leadership - lock expired (TTL: {}ms)", nodeId, remainingTimeMs);
            return false;
        }

        // If TTL is less than 1/3 of the original, renew it
        // This ensures we renew before it expires
        long originalTtlMs = 10000; // 10 seconds from config
        if (remainingTimeMs < (originalTtlMs / 3)) {
            // Unlock and re-acquire with fresh TTL
            lock.unlock();
            boolean reacquired = lock.tryLock(0, originalTtlMs, TimeUnit.MILLISECONDS);

            if (reacquired) {
                log.debug("Node {} renewed leadership (new TTL: {}ms)", nodeId, originalTtlMs);
                return true;
            } else {
                log.error("Node {} failed to renew leadership - another node acquired the lock!", nodeId);
                return false;
            }
        }

        // TTL is still healthy, no need to renew yet
        log.debug("Node {} leadership verified (remaining TTL: {}ms)", nodeId, remainingTimeMs);
        return true;

    } catch (Exception e) {
        log.error("Error renewing leadership for node {}", nodeId, e);
        return false;
    }
}
```

---

### **Step 2: Add Better Logging**

Update `LeaderElectionService.attemptLeaderElection()` to add more diagnostic logging:

```java
private void attemptLeaderElection() {
    if (!isRunning.get()) {
        return;
    }

    try {
        String nodeId = properties.getNode().getId();
        Duration ttl = Duration.ofSeconds(properties.getLeaderElection().getLockTtlSeconds());

        if (isLeader.get()) {
            // Renew leadership
            log.debug("[{}] Attempting to renew leadership", nodeId);
            boolean renewed = coordinationService.renewLeadership(nodeId);
            if (renewed) {
                log.debug("[{}] Leadership renewed successfully", nodeId);
                updateHeartbeat();
            } else {
                log.warn("[{}] Failed to renew leadership - transitioning to follower", nodeId);
                transitionToFollower();
            }
        } else {
            // Try to become leader
            log.debug("[{}] Attempting to acquire leadership", nodeId);
            boolean acquired = coordinationService.tryAcquireLeadership(nodeId, ttl);
            if (acquired) {
                log.info("[{}] Successfully acquired leadership!", nodeId);
                transitionToLeader();
            } else {
                log.trace("[{}] Failed to acquire leadership (another node is leader)", nodeId);
            }
        }
    } catch (Exception e) {
        log.error("Error during leader election", e);
    }
}
```

---

### **Step 3: Add Configuration Validation**

Update `SchedulerProperties.java` to validate the heartbeat interval:

```java
@Data
public static class LeaderElection {
    private boolean enabled = true;

    @NotBlank
    private String lockKey = "scheduler:leader";

    @Min(1)
    private int lockTtlSeconds = 10;

    @Min(1)
    private int heartbeatIntervalSeconds = 3;

    /**
     * Validates that heartbeat interval is less than TTL/3.
     * This ensures at least 2 heartbeats before lock expiry.
     */
    @PostConstruct
    public void validate() {
        if (heartbeatIntervalSeconds > (lockTtlSeconds / 3)) {
            throw new IllegalStateException(
                String.format(
                    "Heartbeat interval (%ds) must be less than lockTtl/3 (%ds) to prevent lock expiry",
                    heartbeatIntervalSeconds,
                    lockTtlSeconds / 3
                )
            );
        }
    }
}
```

---

### **Step 4: Add Health Check Endpoint**

Create a new endpoint to check leader election status:

```java
@RestController
@RequestMapping("/api/v1/cluster")
public class ClusterController {

    private final LeaderElectionService leaderElectionService;
    private final CoordinationService coordinationService;

    @GetMapping("/leader/health")
    public ResponseEntity<LeaderHealthResponse> getLeaderHealth() {
        String currentNodeId = leaderElectionService.getCurrentNode().getNodeId();
        boolean isLeader = leaderElectionService.isLeader();
        boolean holdsLock = coordinationService.isLeader(currentNodeId);

        Optional<String> redisLeader = coordinationService.getCurrentLeader();

        LeaderHealthResponse response = new LeaderHealthResponse(
            currentNodeId,
            isLeader,
            holdsLock,
            redisLeader.orElse(null),
            isLeader == holdsLock  // Consistency check
        );

        return ResponseEntity.ok(response);
    }

    public record LeaderHealthResponse(
        String nodeId,
        boolean isLeaderInMemory,
        boolean holdsRedisLock,
        String redisLeaderNodeId,
        boolean consistent
    ) {}
}
```

---

## 🧪 Testing the Fix

### **Test 1: Single Node Startup**

```bash
# Start only node 1
docker-compose up -d scheduler-node-1

# Wait 5 seconds
sleep 5

# Check if it became leader
docker logs scheduler-node-1 | grep -i "leader"

# Expected output:
# "Node scheduler-node-1 acquired leadership with TTL 10s"
# "Node scheduler-node-1 transitioning to LEADER"
# "Node scheduler-node-1 is now LEADER with epoch 1"
```

### **Test 2: Multi-Node Startup**

```bash
# Start all nodes simultaneously
docker-compose up -d

# Wait 10 seconds
sleep 10

# Check which node is leader
docker logs scheduler-node-1 | grep "LEADER"
docker logs scheduler-node-2 | grep "LEADER"
docker logs scheduler-node-3 | grep "LEADER"

# Expected: Only ONE node should have "is now LEADER" message
```

### **Test 3: Leader Failover**

```bash
# Identify current leader
curl http://localhost:8080/api/v1/cluster/leader

# Kill the leader
docker stop scheduler-node-1

# Wait for TTL to expire (10 seconds)
sleep 12

# Check if new leader elected
curl http://localhost:8081/api/v1/cluster/leader

# Expected: Node 2 or 3 should be the new leader
```

### **Test 4: Redis Lock Verification**

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Check lock exists
EXISTS scheduler:leader:election
# Expected: 1

# Check TTL
TTL scheduler:leader:election
# Expected: 7-10 (seconds remaining)

# Wait for heartbeat interval (3 seconds)
sleep 3

# Check TTL again (should be renewed)
TTL scheduler:leader:election
# Expected: 7-10 (renewed)
```

---

## 📈 Monitoring and Alerts

### **Metrics to Track**

1. **Leader Election Frequency**
   - How often does leadership change?
   - Expected: Very rarely (only on node failure)

2. **Lock Renewal Success Rate**
   - How often does `renewLeadership()` succeed?
   - Expected: 100% for healthy leader

3. **Time Without Leader**
   - How long does the cluster operate without a leader?
   - Expected: < 10 seconds (1 TTL cycle)

4. **Database vs Redis Consistency**
   - Does the database LEADER match the Redis lock holder?
   - Expected: 100% consistency

### **Alerts to Configure**

```yaml
# Prometheus alert rules
groups:
  - name: leader_election
    rules:
      - alert: NoLeaderElected
        expr: count(scheduler_node_role{role="LEADER"}) == 0
        for: 30s
        annotations:
          summary: "No leader elected in cluster"

      - alert: MultipleLeaders
        expr: count(scheduler_node_role{role="LEADER"}) > 1
        for: 10s
        annotations:
          summary: "Multiple leaders detected (split-brain)"

      - alert: LeaderFlapping
        expr: rate(scheduler_leader_elections_total[5m]) > 0.1
        for: 5m
        annotations:
          summary: "Leader election happening too frequently"
```

---

## 🔍 Debugging Checklist

If leader election still fails after the fix:

- [ ] **Redis is running**: `docker ps | grep redis`
- [ ] **Redis is accessible**: `docker exec -it redis redis-cli PING`
- [ ] **Application can connect to Redis**: Check logs for "Redisson" connection messages
- [ ] **TTL is configured correctly**: `lockTtlSeconds = 10`, `heartbeatIntervalSeconds = 3`
- [ ] **No firewall blocking Redis**: Port 6379 is accessible
- [ ] **Redisson client is initialized**: Check for `RedissonClient` bean creation
- [ ] **Leader election is enabled**: `scheduler.leader-election.enabled = true`
- [ ] **Nodes have unique IDs**: Check `scheduler.node.id` in each node's config
- [ ] **Database is accessible**: Check for JPA connection errors
- [ ] **Flyway migrations ran**: Check `scheduler_nodes` table exists

---

## 📚 Additional Resources

- **Redisson Documentation**: https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers
- **Redis TTL Commands**: https://redis.io/commands/ttl/
- **Leader Election Patterns**: https://martinfowler.com/articles/patterns-of-distributed-systems/leader-follower.html

---

**Status**: Fix ready to implement
**Priority**: HIGH
**Estimated Fix Time**: 30 minutes
**Testing Time**: 15 minutes
**Total Time**: 45 minutes

