# Heartbeat & Leader Election - Quick Reference Card

**Last Updated**: 2026-03-12

---

## 🎯 One-Sentence Summary

**Redis lock renewal** (leader only) determines **who is leader RIGHT NOW**, while **database heartbeats** (all nodes) provide **observability and monitoring**.

---

## 📊 Quick Comparison

| | Redis Lock Renewal | Database Heartbeats |
|---|---|---|
| **Who?** | Leader only | All nodes (leader + followers) |
| **Where?** | Redis (in-memory) | MySQL (persistent) |
| **Why?** | Leader election & failover | Observability & monitoring |
| **How often?** | Every 3 seconds | Every 3 seconds |
| **Authoritative?** | ✅ YES (source of truth) | ❌ NO (informational) |
| **Survives restart?** | ❌ No | ✅ Yes |

---

## ⏱️ Timing Configuration

```yaml
scheduler:
  leader-election:
    lock-ttl-seconds: 10        # Redis lock expires after 10s
    heartbeat-interval-seconds: 3  # Renew/heartbeat every 3s
```

**Math**: `TTL / Interval = 10 / 3 = 3.33`
- Leader can miss **2 heartbeats** before losing leadership
- Provides **fault tolerance** for transient network issues
- Ensures **fast failover** (max 10 seconds)

---

## 🔄 What Happens Every 3 Seconds?

### **Leader Node**

```
LeaderElectionService (every 3s):
├─ Renew Redis lock (TTL=10s)
└─ If successful: Update database heartbeat

HeartbeatService (every 3s):
└─ Update database heartbeat (last_heartbeat_at)
```

**Result**: Both Redis AND database are updated

---

### **Follower Node**

```
LeaderElectionService (every 3s):
├─ Try to acquire Redis lock
└─ Usually fails (leader holds it)

HeartbeatService (every 3s):
└─ Update database heartbeat (last_heartbeat_at)
```

**Result**: Only database is updated (no Redis interaction)

---

## 💥 Failure Scenarios

### **Leader Crashes**

```
t=0s:  Leader holds Redis lock (TTL=10s)
t=3s:  Leader renews lock (TTL=10s)
t=6s:  Leader renews lock (TTL=10s)
t=7s:  💥 LEADER CRASHES
t=9s:  Followers try to acquire (lock still exists, TTL=8s)
t=12s: Followers try to acquire (lock still exists, TTL=5s)
t=17s: Lock expires! Follower 1 acquires lock → NEW LEADER
t=30s: Stale detection marks old leader as unhealthy in database
```

**Failover time**: **10 seconds** (1 TTL cycle)

---

### **Redis Fails**

```
✅ Leader continues executing jobs (already in memory)
❌ No new leaders can be elected
✅ Database heartbeats continue (observability maintained)
✅ Operators can see which nodes are alive
→ Manual intervention or Redis restart needed
```

---

### **Database Fails**

```
✅ Leader election continues (Redis still works)
✅ Jobs continue executing
❌ Lose observability (can't query cluster state)
❌ Monitoring dashboards show stale data
→ Application continues, but blind to cluster health
```

---

## 🔍 Key Methods

### **LeaderElectionService**

```java
// Runs every 3 seconds
private void attemptLeaderElection() {
    if (isLeader.get()) {
        // LEADER: Renew Redis lock
        boolean renewed = coordinationService.renewLeadership(nodeId);
        if (renewed) {
            updateHeartbeat();  // Update database
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
}
```

---

### **HeartbeatService**

```java
// Runs every 3 seconds (ALL nodes)
@Transactional
protected void sendHeartbeat() {
    SchedulerNode node = leaderElectionService.getCurrentNode();
    node.recordHeartbeat();  // Updates last_heartbeat_at
    nodeRepository.save(node);
}

// Runs every 30 seconds
@Transactional
protected void detectStaleNodes() {
    long staleThresholdSeconds = 3 * 3;  // 9 seconds
    
    for (SchedulerNode node : healthyNodes) {
        if (node.isHeartbeatStale(staleThresholdSeconds)) {
            node.markUnhealthy();
            if (node.getRole() == LEADER) {
                node.demoteToFollower();  // Database cleanup only
            }
            nodeRepository.save(node);
        }
    }
}
```

---

## 🎓 Interview Answers

### **"Why two mechanisms?"**

> "Redis provides fast, atomic leader election with automatic failover via TTL expiry. Database provides persistent observability and historical records. Together, they give me reliable coordination with comprehensive monitoring."

---

### **"What if Redis fails?"**

> "Leader election stops, but the current leader continues working. Database heartbeats continue, so operators maintain visibility. This allows manual intervention without losing cluster health data."

---

### **"Why heartbeat interval = TTL / 3?"**

> "This allows the leader to miss 2 consecutive heartbeats and still maintain leadership. It provides fault tolerance for transient network issues while ensuring fast failover within 10 seconds if the leader truly crashes."

---

### **"How do you prevent split-brain?"**

> "Three mechanisms: (1) Redis locks are atomic - only one node can hold the lock. (2) Epoch numbers (fencing tokens) - each leader election increments the epoch, and the database validates writes come from the current epoch. (3) Stale node detection marks old leaders as unhealthy."

---

## 📈 Monitoring Queries

### **Check Current Leader**

```sql
SELECT node_id, role, epoch, last_heartbeat_at 
FROM scheduler_nodes 
WHERE role = 'LEADER';
```

**Expected**: Exactly 1 row

---

### **Check All Nodes Health**

```sql
SELECT 
    node_id, 
    role, 
    healthy,
    TIMESTAMPDIFF(SECOND, last_heartbeat_at, NOW()) as seconds_since_heartbeat
FROM scheduler_nodes
ORDER BY role DESC, last_heartbeat_at DESC;
```

**Expected**: All nodes have `seconds_since_heartbeat < 10`

---

### **Check Redis Lock**

```bash
# Check if lock exists
docker exec redis redis-cli EXISTS scheduler:leader:election

# Check TTL
docker exec redis redis-cli TTL scheduler:leader:election
```

**Expected**: TTL between 1-10 seconds (not -1)

---

## 🚨 Alerts to Configure

```yaml
# Prometheus alert rules
- alert: NoLeaderElected
  expr: count(scheduler_node_role{role="LEADER"}) == 0
  for: 30s
  
- alert: MultipleLeaders
  expr: count(scheduler_node_role{role="LEADER"}) > 1
  for: 10s
  
- alert: StaleHeartbeat
  expr: time() - scheduler_node_last_heartbeat > 15
  for: 30s
  
- alert: RedisLockInfinite
  expr: scheduler_redis_lock_ttl == -1
  for: 10s
```

---

## 📚 Related Documentation

- **Detailed Explanation**: `docs/HEARTBEAT_VS_LEADER_ELECTION.md`
- **Leader Election Fix**: `docs/LEADER_ELECTION_FIX_APPLIED.md`
- **Architecture**: `ARCHITECTURE.md`

---

**Remember**: Redis = **Coordination** (who is leader), Database = **Observability** (cluster health history)

