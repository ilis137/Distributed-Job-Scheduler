# Retry Flow: Before vs. After Fix

**Date**: 2026-03-08

---

## 🔴 Before Fix: Broken Retry Flow

### **Timeline**

```
T=0s    ┌─────────────────────────────────────────┐
        │ Job Execution Fails                     │
        │ - Exception thrown during execution     │
        │ - JobExecutor.handleFailedExecution()   │
        └─────────────────────────────────────────┘
                        ↓
T=0s    ┌─────────────────────────────────────────┐
        │ Mark Job as FAILED                      │
        │ - jobService.failJob()                  │
        │ - status = FAILED                       │
        │ - retryCount = 1                        │
        └─────────────────────────────────────────┘
                        ↓
T=0s    ┌─────────────────────────────────────────┐
        │ Schedule Retry                          │
        │ - retryManager.calculateNextRetryTime() │
        │ - backoff = 30 seconds                  │
        │ - jobService.retryJob()                 │
        │ - status = RETRYING ✅                  │
        │ - nextRunTime = now + 30s ✅            │
        └─────────────────────────────────────────┘
                        ↓
T=1s    ┌─────────────────────────────────────────┐
        │ JobScheduler Polls (every 1 second)     │
        │ - jobService.findDueJobs(100)           │
        │ - Query: WHERE status = 'PENDING'       │
        │ - Result: [] (empty)                    │
        │ ❌ RETRYING jobs excluded!              │
        └─────────────────────────────────────────┘
                        ↓
T=30s   ┌─────────────────────────────────────────┐
        │ JobScheduler Polls                      │
        │ - Query: WHERE status = 'PENDING'       │
        │   AND nextRunTime <= now                │
        │ - Result: [] (empty)                    │
        │ ❌ Still excludes RETRYING!             │
        └─────────────────────────────────────────┘
                        ↓
T=∞     ┌─────────────────────────────────────────┐
        │ Job Stuck Forever                       │
        │ - status = RETRYING                     │
        │ - Never re-executed                     │
        │ ❌ BROKEN!                              │
        └─────────────────────────────────────────┘
```

### **Database State**

```sql
-- After retry is scheduled (T=0s)
SELECT id, name, status, retry_count, next_run_time 
FROM jobs 
WHERE id = 1;

+----+--------+-----------+-------------+---------------------+
| id | name   | status    | retry_count | next_run_time       |
+----+--------+-----------+-------------+---------------------+
| 1  | job-1  | RETRYING  | 1           | 2026-03-08 11:00:30 |
+----+--------+-----------+-------------+---------------------+

-- Polling query at T=30s
SELECT j.* FROM jobs j
WHERE j.status = 'PENDING'  -- ❌ Excludes RETRYING!
  AND j.enabled = true
  AND j.next_run_time <= '2026-03-08 11:00:30';

-- Result: 0 rows (job not found!)
```

---

## 🟢 After Fix: Working Retry Flow

### **Timeline**

```
T=0s    ┌─────────────────────────────────────────┐
        │ Job Execution Fails                     │
        │ - Exception thrown during execution     │
        │ - JobExecutor.handleFailedExecution()   │
        └─────────────────────────────────────────┘
                        ↓
T=0s    ┌─────────────────────────────────────────┐
        │ Mark Job as FAILED                      │
        │ - jobService.failJob()                  │
        │ - status = FAILED                       │
        │ - retryCount = 1                        │
        └─────────────────────────────────────────┘
                        ↓
T=0s    ┌─────────────────────────────────────────┐
        │ Schedule Retry                          │
        │ - retryManager.calculateNextRetryTime() │
        │ - backoff = 30 seconds                  │
        │ - jobService.retryJob()                 │
        │ - status = RETRYING ✅                  │
        │ - nextRunTime = now + 30s ✅            │
        └─────────────────────────────────────────┘
                        ↓
T=1s    ┌─────────────────────────────────────────┐
        │ JobScheduler Polls (every 1 second)     │
        │ - jobService.findDueJobs(100)           │
        │ - Query: WHERE status IN                │
        │   ('PENDING', 'RETRYING')               │
        │ - Result: [] (empty - too early)        │
        │ ✅ nextRunTime prevents immediate retry │
        └─────────────────────────────────────────┘
                        ↓
T=30s   ┌─────────────────────────────────────────┐
        │ JobScheduler Polls                      │
        │ - Query: WHERE status IN                │
        │   ('PENDING', 'RETRYING')               │
        │   AND nextRunTime <= now                │
        │ - Result: [Job(id=1, status=RETRYING)]  │
        │ ✅ Job found!                           │
        └─────────────────────────────────────────┘
                        ↓
T=30s   ┌─────────────────────────────────────────┐
        │ Submit Job for Execution                │
        │ - jobService.scheduleJob()              │
        │ - status = SCHEDULED                    │
        └─────────────────────────────────────────┘
                        ↓
T=30s   ┌─────────────────────────────────────────┐
        │ Acquire Lock & Start Execution          │
        │ - lockService.tryAcquire()              │
        │ - jobService.startJob()                 │
        │ - status = RUNNING                      │
        └─────────────────────────────────────────┘
                        ↓
T=35s   ┌─────────────────────────────────────────┐
        │ Job Completes Successfully              │
        │ - jobService.completeJob()              │
        │ - status = COMPLETED ✅                 │
        │ - retryCount reset to 0                 │
        └─────────────────────────────────────────┘
```

### **Database State**

```sql
-- After retry is scheduled (T=0s)
SELECT id, name, status, retry_count, next_run_time 
FROM jobs 
WHERE id = 1;

+----+--------+-----------+-------------+---------------------+
| id | name   | status    | retry_count | next_run_time       |
+----+--------+-----------+-------------+---------------------+
| 1  | job-1  | RETRYING  | 1           | 2026-03-08 11:00:30 |
+----+--------+-----------+-------------+---------------------+

-- Polling query at T=30s (FIXED!)
SELECT j.* FROM jobs j
WHERE j.status IN ('PENDING', 'RETRYING')  -- ✅ Includes RETRYING!
  AND j.enabled = true
  AND j.next_run_time <= '2026-03-08 11:00:30';

-- Result: 1 row (job found!)
+----+--------+-----------+-------------+---------------------+
| id | name   | status    | retry_count | next_run_time       |
+----+--------+-----------+-------------+---------------------+
| 1  | job-1  | RETRYING  | 1           | 2026-03-08 11:00:30 |
+----+--------+-----------+-------------+---------------------+

-- After successful retry (T=35s)
+----+--------+-----------+-------------+---------------------+
| id | name   | status    | retry_count | next_run_time       |
+----+--------+-----------+-------------+---------------------+
| 1  | job-1  | COMPLETED | 0           | NULL                |
+----+--------+-----------+-------------+---------------------+
```

---

## 📊 Side-by-Side Comparison

| Aspect | Before Fix ❌ | After Fix ✅ |
|--------|--------------|-------------|
| **Query** | `WHERE status = 'PENDING'` | `WHERE status IN ('PENDING', 'RETRYING')` |
| **RETRYING jobs found?** | ❌ No | ✅ Yes |
| **Exponential backoff works?** | N/A (never retried) | ✅ Yes (nextRunTime prevents immediate retry) |
| **Job re-executed?** | ❌ No (stuck forever) | ✅ Yes (after backoff period) |
| **Manual intervention needed?** | ✅ Yes (reset job manually) | ❌ No (automatic retry) |
| **Production ready?** | ❌ No (critical bug) | ✅ Yes (works correctly) |

---

## 🔄 State Transitions

### **Before Fix** ❌

```
PENDING → SCHEDULED → RUNNING → FAILED → RETRYING → [STUCK FOREVER]
                                                      ❌ Dead end!
```

### **After Fix** ✅

```
PENDING → SCHEDULED → RUNNING → FAILED → RETRYING → SCHEDULED → RUNNING → COMPLETED
                                            ↑                        ↓
                                            └────── (retry) ─────────┘
                                                   ✅ Complete cycle!
```

---

## 🎯 Key Takeaway

**The Fix**: One line change in the query
```java
// Before
WHERE j.status = 'PENDING'

// After
WHERE j.status IN ('PENDING', 'RETRYING')
```

**The Impact**: Complete retry flow now works end-to-end! ✅

