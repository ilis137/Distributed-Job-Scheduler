# Leader Election - Quick Fix Guide

**Problem**: All nodes are FOLLOWER, no LEADER elected  
**Time to Fix**: 5 minutes

---

## 🚨 Immediate Diagnostic (30 seconds)

### **Step 1: Check Redis Lock**

```bash
docker exec -it redis redis-cli EXISTS scheduler:leader:election
```

**Result**:
- `1` = Lock exists (someone holds it)
- `0` = No lock (no leader elected)

### **Step 2: Check Lock TTL**

```bash
docker exec -it redis redis-cli TTL scheduler:leader:election
```

**Result**:
- `-1` = Lock never expires (WATCHDOG ENABLED - THIS IS THE BUG!)
- `-2` = Lock doesn't exist
- `1-10` = Lock expires in N seconds (CORRECT)

### **Step 3: Check Database**

```bash
docker exec -it mysql mysql -u scheduler -pscheduler123 scheduler_db -e \
  "SELECT node_id, role, epoch FROM scheduler_nodes;"
```

**Expected**: ONE node with `role='LEADER'`  
**Actual (Bug)**: ALL nodes with `role='FOLLOWER'`

---

## ⚡ Quick Fix (2 minutes)

### **Option A: Force Re-Election (Fastest)**

```bash
# 1. Delete the Redis lock
docker exec -it redis redis-cli DEL scheduler:leader:election

# 2. Wait 5 seconds for next election cycle
sleep 5

# 3. Verify leader elected
docker exec -it mysql mysql -u scheduler -pscheduler123 scheduler_db -e \
  "SELECT node_id, role FROM scheduler_nodes WHERE role='LEADER';"
```

**Expected Output**: One node with `LEADER` role

---

### **Option B: Restart All Nodes**

```bash
# Restart all scheduler nodes
docker-compose restart scheduler-node-1 scheduler-node-2 scheduler-node-3

# Wait 10 seconds
sleep 10

# Check leader
curl http://localhost:8080/api/v1/cluster/leader
```

---

## 🔧 Permanent Fix (3 minutes)

### **Edit `RedisCoordinationService.java`**

**File**: `src/main/java/com/scheduler/coordination/RedisCoordinationService.java`

**Line 57**: Change from:
```java
boolean acquired = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
```

**To**:
```java
boolean acquired = lock.tryLock(0, ttl.toMillis(), TimeUnit.MILLISECONDS);
```

**Why**: `-1` enables Redisson's watchdog (infinite TTL), but we want explicit TTL for failover.

---

### **Rebuild and Restart**

```bash
# Rebuild Docker image
docker-compose build scheduler-node-1

# Restart all nodes
docker-compose down
docker-compose up -d

# Verify fix
sleep 10
curl http://localhost:8080/api/v1/cluster/leader
```

---

## ✅ Verification (1 minute)

### **Test 1: Check Redis TTL**

```bash
docker exec -it redis redis-cli TTL scheduler:leader:election
```

**Expected**: `7-10` (seconds remaining, not `-1`)

### **Test 2: Check Database**

```bash
docker exec -it mysql mysql -u scheduler -pscheduler123 scheduler_db -e \
  "SELECT node_id, role, epoch, last_heartbeat FROM scheduler_nodes ORDER BY last_heartbeat DESC;"
```

**Expected**:
- ONE node with `role='LEADER'`
- Other nodes with `role='FOLLOWER'`
- All nodes have recent `last_heartbeat` (within last 5 seconds)

### **Test 3: Check Application Logs**

```bash
docker logs scheduler-node-1 --tail 20 | grep -i "leader"
```

**Expected Output**:
```
Node scheduler-node-1 acquired leadership with TTL 10s
Node scheduler-node-1 transitioning to LEADER
Node scheduler-node-1 is now LEADER with epoch 1
```

---

## 🐛 If Still Not Working

### **Check 1: Redis Connection**

```bash
docker logs scheduler-node-1 | grep -i "redis"
```

Look for: `Redisson client initialized` or connection errors

### **Check 2: Leader Election Enabled**

```bash
docker exec scheduler-node-1 cat /app/application.yml | grep -A 5 "leader-election"
```

Verify: `enabled: true`

### **Check 3: Unique Node IDs**

```bash
docker logs scheduler-node-1 | grep "node:"
docker logs scheduler-node-2 | grep "node:"
docker logs scheduler-node-3 | grep "node:"
```

Each node should have a different ID (e.g., `scheduler-node-1`, `scheduler-node-2`, `scheduler-node-3`)

---

## 📞 Need More Help?

See detailed diagnosis: `docs/LEADER_ELECTION_DIAGNOSIS.md`

---

**Last Updated**: 2026-03-12  
**Status**: Ready to apply

