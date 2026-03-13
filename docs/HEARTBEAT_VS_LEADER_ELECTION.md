# HeartbeatService vs LeaderElectionService - Design Rationale

**Date**: 2026-03-12  
**Topic**: Understanding the dual-mechanism approach to failure detection

---

## 🎯 Quick Answer

**Why two mechanisms?**

1. **Redis Lock Renewal** (LeaderElectionService) = **Source of Truth** for leadership
2. **Database Heartbeats** (HeartbeatService) = **Observability & Monitoring** for cluster health

They serve **different purposes** and work together to provide **reliable leader election** with **comprehensive observability**.

---

## 📊 Side-by-Side Comparison

| Aspect | Redis Lock Renewal | Database Heartbeats |
|--------|-------------------|---------------------|
| **Service** | `LeaderElectionService` | `HeartbeatService` |
| **Storage** | Redis (in-memory) | MySQL (persistent) |
| **Purpose** | Leader election & failover | Observability & monitoring |
| **Frequency** | Every 3 seconds | Every 3 seconds |
| **Who Updates** | **Leader only** (when renewing) | **All nodes** (leader + followers) |
| **Failure Detection** | Automatic (TTL expires) | Manual (stale detection every 30s) |
| **Source of Truth** | ✅ **YES** (authoritative) | ❌ **NO** (informational) |
| **Survives Restart** | ❌ No (in-memory) | ✅ Yes (persistent) |
| **Query Performance** | Very fast (Redis) | Slower (MySQL) |
| **Use Case** | Distributed coordination | Debugging & dashboards |

---

## 🔍 Detailed Explanation

### **1. Purpose of Each Mechanism**

#### **Redis Lock Renewal (LeaderElectionService)**

**Purpose**: **Distributed coordination** - determines who is the leader RIGHT NOW

**How it works**:
```java
// LeaderElectionService.attemptLeaderElection() - runs every 3 seconds
if (isLeader.get()) {
    // LEADER: Renew the Redis lock to maintain leadership
    boolean renewed = coordinationService.renewLeadership(nodeId);
    if (renewed) {
        updateHeartbeat();  // Also update database for observability
    } else {
        transitionToFollower();  // Lost leadership!
    }
} else {
    // FOLLOWER: Try to acquire leadership
    boolean acquired = coordinationService.tryAcquireLeadership(nodeId, ttl);
    if (acquired) {
        transitionToLeader();  // Became leader!
    }
}
```

**Key Points**:
- Only the **current leader** renews the Redis lock
- Lock has **10-second TTL** - expires if not renewed
- If leader crashes, lock expires automatically → followers compete for leadership
- **Atomic operation** - only one node can hold the lock
- **Fast failover** - new leader elected within 10 seconds

---

#### **Database Heartbeats (HeartbeatService)**

**Purpose**: **Observability & monitoring** - track cluster health over time

**How it works**:
```java
// HeartbeatService.sendHeartbeat() - runs every 3 seconds
@Transactional
protected void sendHeartbeat() {
    SchedulerNode node = leaderElectionService.getCurrentNode();
    
    if (node != null) {
        node.recordHeartbeat();  // Updates last_heartbeat_at timestamp
        nodeRepository.save(node);
        
        log.debug("Heartbeat sent for node: {} (role: {}, epoch: {})",
            nodeId, node.getRole(), node.getEpoch());
    }
}
```

**Key Points**:
- **All nodes** (leader + followers) send heartbeats
- Updates `last_heartbeat_at` timestamp in database
- Provides **historical record** of node activity
- Enables **monitoring dashboards** to show cluster health
- Helps **debug issues** by showing when nodes were last active
- **Not used for leader election** - purely informational

---

### **2. Why Two Separate Mechanisms?**

#### **Separation of Concerns**

**Redis (Coordination)**:
- **Fast, in-memory** - optimized for distributed locking
- **TTL-based expiry** - automatic failover without manual intervention
- **Atomic operations** - prevents split-brain scenarios
- **Ephemeral** - state disappears on restart (by design)

**Database (Observability)**:
- **Persistent** - survives restarts and crashes
- **Queryable** - can analyze historical data
- **Relational** - can join with jobs, executions, etc.
- **Auditable** - provides paper trail for debugging

#### **Redundancy & Reliability**

If Redis fails:
- Leader election stops working (no new leaders elected)
- **But** database heartbeats continue
- Operators can see which nodes are still alive
- Can manually intervene or restart Redis

If Database fails:
- Leader election continues working (Redis still operational)
- Jobs can still execute
- **But** lose observability (can't query cluster state)
- Monitoring dashboards show stale data

---

### **3. Heartbeats: Leader vs Follower**

#### **Leader Node Behavior**

```java
// Every 3 seconds, leader does TWO things:

// 1. Renew Redis lock (LeaderElectionService)
boolean renewed = coordinationService.renewLeadership(nodeId);

// 2. Update database heartbeat (called from LeaderElectionService)
if (renewed) {
    updateHeartbeat();  // Updates last_heartbeat_at in database
}
```

**Leader heartbeat flow**:
1. `LeaderElectionService.attemptLeaderElection()` runs (every 3s)
2. Calls `coordinationService.renewLeadership()` → renews Redis lock
3. If successful, calls `updateHeartbeat()` → updates database
4. **Result**: Both Redis lock AND database are updated

---

#### **Follower Node Behavior**

```java
// Every 3 seconds, follower does TWO things:

// 1. Try to acquire leadership (LeaderElectionService)
boolean acquired = coordinationService.tryAcquireLeadership(nodeId, ttl);
// Usually fails (leader already holds lock)

// 2. Send database heartbeat (HeartbeatService - separate service!)
sendHeartbeat();  // Updates last_heartbeat_at in database
```

**Follower heartbeat flow**:
1. `LeaderElectionService.attemptLeaderElection()` runs (every 3s)
2. Calls `tryAcquireLeadership()` → fails (leader holds lock)
3. **Separately**, `HeartbeatService.sendHeartbeat()` runs (every 3s)
4. Updates `last_heartbeat_at` in database
5. **Result**: Only database is updated (no Redis lock)

---

### **4. Failure Detection & Stale Node Cleanup**

#### **Redis-Based Failure Detection (Automatic)**

```java
// In RedisCoordinationService.renewLeadership()
long remainingTimeMs = lock.remainTimeToLive();

if (remainingTimeMs <= 0) {
    log.warn("Node {} lost leadership - lock expired", nodeId);
    return false;  // Leader lost leadership!
}
```

**How it works**:
- Leader renews lock every 3 seconds
- Lock has 10-second TTL
- If leader crashes, lock expires after 10 seconds
- **Automatic** - no manual intervention needed
- Followers immediately compete for leadership

**Failure detection time**: **10 seconds** (1 TTL cycle)

---

#### **Database-Based Failure Detection (Manual)**

```java
// In HeartbeatService.detectStaleNodes() - runs every 30 seconds
@Transactional
protected void detectStaleNodes() {
    int intervalSeconds = 3;  // Heartbeat interval
    long staleThresholdSeconds = intervalSeconds * 3L;  // 9 seconds
    
    List<SchedulerNode> healthyNodes = nodeRepository.findHealthyNodes();
    
    for (SchedulerNode node : healthyNodes) {
        if (node.isHeartbeatStale(staleThresholdSeconds)) {
            log.warn("Detected stale node: {}", node.getNodeId());
            
            node.markUnhealthy();
            
            // If it was a leader, demote it in database
            if (node.getRole() == NodeRole.LEADER) {
                log.warn("Stale leader detected: {} - demoting", node.getNodeId());
                node.demoteToFollower();
            }
            
            nodeRepository.save(node);
        }
    }
}
```

**How it works**:
- Runs every **30 seconds** (not every 3 seconds!)
- Checks if `last_heartbeat_at` is older than **9 seconds** (3 intervals)
- Marks node as `healthy = false`
- If node was leader, demotes to follower **in database only**
- **Does NOT affect Redis lock** - this is just cleanup

**Failure detection time**: **30-39 seconds** (next stale detection cycle)

---

### **5. Relationship Between Heartbeat Interval and Redis TTL**

#### **Configuration**

```yaml
# application.yml
scheduler:
  leader-election:
    lock-ttl-seconds: 10        # Redis lock TTL
    heartbeat-interval-seconds: 3  # Both Redis renewal AND database heartbeat
```

#### **The Math**

```
Heartbeat Interval = 3 seconds
Redis Lock TTL = 10 seconds

Ratio: TTL / Interval = 10 / 3 = 3.33

This means:
- Leader renews lock every 3 seconds
- Lock expires after 10 seconds
- Leader can miss 2 heartbeats before losing leadership
- Provides tolerance for transient network issues
```

#### **Why This Ratio?**

**Industry Best Practice**: Heartbeat interval should be **TTL / 3**

**Reasoning**:
1. **First heartbeat** (t=0s): Lock acquired, TTL = 10s
2. **Second heartbeat** (t=3s): Lock renewed, TTL = 10s
3. **Third heartbeat** (t=6s): Lock renewed, TTL = 10s
4. **Fourth heartbeat** (t=9s): Lock renewed, TTL = 10s
5. **If leader crashes at t=9.5s**: Lock expires at t=10s (only 0.5s delay!)

**Benefits**:
- **Fast failover**: Max 10 seconds to detect leader failure
- **Tolerates missed heartbeats**: Can miss 2 heartbeats (6s) and still maintain leadership
- **Prevents false positives**: Transient network glitches don't cause unnecessary failovers

---

## 🔄 Complete Flow Diagram

### **Normal Operation (Leader Alive)**

```
Time: 0s
├─ Leader: Acquire Redis lock (TTL=10s)
├─ Leader: Transition to LEADER in database
└─ Leader: Send database heartbeat

Time: 3s (Heartbeat #1)
├─ Leader: Renew Redis lock (TTL=10s)
├─ Leader: Update database heartbeat
├─ Follower 1: Try acquire lock (FAIL)
├─ Follower 1: Send database heartbeat
├─ Follower 2: Try acquire lock (FAIL)
└─ Follower 2: Send database heartbeat

Time: 6s (Heartbeat #2)
├─ Leader: Renew Redis lock (TTL=10s)
├─ Leader: Update database heartbeat
├─ Follower 1: Try acquire lock (FAIL)
├─ Follower 1: Send database heartbeat
├─ Follower 2: Try acquire lock (FAIL)
└─ Follower 2: Send database heartbeat

Time: 9s (Heartbeat #3)
├─ Leader: Renew Redis lock (TTL=10s)
├─ Leader: Update database heartbeat
├─ Follower 1: Try acquire lock (FAIL)
├─ Follower 1: Send database heartbeat
├─ Follower 2: Try acquire lock (FAIL)
└─ Follower 2: Send database heartbeat
```

---

### **Leader Failure Scenario**

```
Time: 0s
└─ Leader: Holding Redis lock (TTL=10s)

Time: 3s
└─ Leader: Renew Redis lock (TTL=10s)

Time: 6s
└─ Leader: Renew Redis lock (TTL=10s)

Time: 7s
└─ 💥 LEADER CRASHES! (No more renewals)

Time: 9s
├─ Follower 1: Try acquire lock (FAIL - lock still exists, TTL=8s)
└─ Follower 2: Try acquire lock (FAIL - lock still exists, TTL=8s)

Time: 12s
├─ Follower 1: Try acquire lock (FAIL - lock still exists, TTL=5s)
└─ Follower 2: Try acquire lock (FAIL - lock still exists, TTL=5s)

Time: 15s
├─ Follower 1: Try acquire lock (FAIL - lock still exists, TTL=2s)
└─ Follower 2: Try acquire lock (FAIL - lock still exists, TTL=2s)

Time: 17s (Lock expires!)
├─ Follower 1: Try acquire lock (SUCCESS! 🎉)
├─ Follower 1: Transition to LEADER in database
├─ Follower 1: Increment epoch (epoch++)
└─ Follower 2: Try acquire lock (FAIL - Follower 1 now holds it)

Time: 30s (Stale detection runs)
├─ HeartbeatService: Detect old leader is stale (last heartbeat at t=6s)
├─ HeartbeatService: Mark old leader as unhealthy
└─ HeartbeatService: Demote old leader to FOLLOWER in database
```

**Failover time**: **10 seconds** (time for Redis lock to expire)

---

## 🎓 Interview Talking Points

### **1. "Why do you have two separate mechanisms?"**

**Answer**:
> "I use Redis for distributed coordination because it's fast and provides atomic operations with TTL-based expiry. This is the source of truth for leadership. However, Redis is ephemeral - the state disappears on restart. So I also track heartbeats in the database for observability and debugging. This gives me the best of both worlds: fast, reliable leader election with Redis, and comprehensive monitoring with the database."

---

### **2. "What happens if Redis fails?"**

**Answer**:
> "If Redis fails, leader election stops working - no new leaders can be elected. However, the current leader continues executing jobs because it's already in memory. Database heartbeats continue, so operators can see which nodes are alive. This allows manual intervention or Redis restart without losing visibility into cluster health."

---

### **3. "Why is the heartbeat interval 1/3 of the TTL?"**

**Answer**:
> "This is an industry best practice for distributed systems. With a 10-second TTL and 3-second heartbeat interval, the leader can miss 2 consecutive heartbeats and still maintain leadership. This provides tolerance for transient network issues while ensuring fast failover - if the leader truly crashes, we detect it within 10 seconds."

---

### **4. "How do you prevent split-brain scenarios?"**

**Answer**:
> "I use three mechanisms: First, Redis locks are atomic - only one node can hold the lock at a time. Second, I use epoch numbers (fencing tokens) - each leader election increments the epoch, and the database validates that writes come from the current epoch. Third, I have stale node detection that marks old leaders as unhealthy in the database, providing a safety net for cleanup."

---

### **5. "What's the difference between leader heartbeats and follower heartbeats?"**

**Answer**:
> "Leaders do two things every heartbeat cycle: renew the Redis lock (to maintain leadership) and update the database heartbeat (for observability). Followers only update the database heartbeat - they don't touch Redis unless they're trying to acquire leadership. This separation ensures followers don't interfere with the leader's lock while still providing visibility into their health status."

---

## 📚 Summary

| Question | Answer |
|----------|--------|
| **Purpose of Redis renewal?** | Source of truth for leadership, enables automatic failover |
| **Purpose of database heartbeats?** | Observability, monitoring, debugging, historical record |
| **Why two mechanisms?** | Separation of concerns: coordination vs observability |
| **Leader heartbeat behavior?** | Renews Redis lock + updates database |
| **Follower heartbeat behavior?** | Only updates database (no Redis interaction) |
| **Failure detection time?** | 10 seconds (Redis TTL expiry) |
| **Stale node cleanup time?** | 30-39 seconds (database cleanup cycle) |
| **Heartbeat interval?** | 3 seconds (TTL / 3 for fault tolerance) |

---

**Key Insight**: Redis and database serve **complementary roles** - Redis for **coordination**, database for **observability**. Together, they provide **reliable leader election** with **comprehensive monitoring**.

