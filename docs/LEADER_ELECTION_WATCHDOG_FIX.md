# Leader Election Watchdog Fix

**Date**: 2026-03-07  
**Status**: ✅ **FIXED**  
**Issue**: Leader lock TTL not being renewed, causing unnecessary leader churn

---

## Problem Description

The original implementation of `renewLeadership()` in `RedisCoordinationService` had a critical bug: it **did not actually extend the Redis lock TTL** when renewing leadership.

### Original Buggy Code

```java
@Override
public boolean renewLeadership(String nodeId) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);
    
    try {
        if (lock.isHeldByCurrentThread()) {
            // ⚠️ BUG: This only checks if lock is held, doesn't renew TTL!
            boolean stillHeld = lock.isLocked() && lock.isHeldByCurrentThread();
            
            if (stillHeld) {
                log.debug("Node {} successfully renewed leadership", nodeId);
                return true;  // ⚠️ Returns true but TTL NOT extended!
            }
        }
    }
    // ...
}
```

### The Problem

1. Leader acquires lock with **10-second TTL** at T=0
2. Leader calls `renewLeadership()` at T=3s, T=6s, T=9s
3. Method returns `true` (lock still held)
4. **BUT**: TTL is **NOT extended** - still expires at T=10s
5. Lock expires at T=10s **even though leader is healthy**
6. Follower acquires lock → **unnecessary leader change**

### Impact

- ⚠️ **Leader churn every ~10 seconds** even when leader is healthy
- ⚠️ **Unnecessary failovers** disrupt job scheduling
- ⚠️ **Epoch increments unnecessarily** on each leader change
- ⚠️ **Database writes** for every leader transition

---

## Solution: Redisson Watchdog Mechanism

### What is the Watchdog?

Redisson's **watchdog** is an automatic lock renewal mechanism:
- When you acquire a lock with **leaseTime = -1**, watchdog is enabled
- Watchdog **automatically renews** the lock in the background
- Renewal happens every **(lockWatchdogTimeout / 3)** milliseconds
- Default `lockWatchdogTimeout` is **30 seconds** → renewal every **10 seconds**
- Watchdog stops when lock is explicitly released or thread dies

### Fixed Implementation

#### 1. Updated `tryAcquireLeadership()` - Enable Watchdog

```java
@Override
public boolean tryAcquireLeadership(String nodeId, Duration ttl) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);
    
    try {
        // ✅ Using -1 for leaseTime enables automatic lock renewal by the watchdog
        // The watchdog renews the lock every (lockWatchdogTimeout / 3) milliseconds
        // Default lockWatchdogTimeout is 30 seconds, so renewal happens every 10 seconds
        boolean acquired = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
        
        if (acquired) {
            log.info("Node {} acquired leadership with watchdog auto-renewal enabled", nodeId);
            return true;
        } else {
            log.debug("Node {} failed to acquire leadership - another node is leader", nodeId);
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
```

#### 2. Updated `renewLeadership()` - Verify Lock Held

```java
@Override
public boolean renewLeadership(String nodeId) {
    String lockKey = LEADER_LOCK_PREFIX + "election";
    RLock lock = redissonClient.getLock(lockKey);
    
    try {
        // ✅ With watchdog enabled, the lock is automatically renewed
        // We just need to verify that this node still holds the lock
        // The watchdog renews the lock in the background every (lockWatchdogTimeout / 3) ms
        if (lock.isHeldByCurrentThread()) {
            // Lock is still held and watchdog is renewing it automatically
            log.debug("Node {} leadership verified - watchdog auto-renewal active", nodeId);
            return true;
        } else {
            // Lock was lost (either expired or released by another process)
            log.warn("Node {} lost leadership - lock no longer held", nodeId);
            return false;
        }
    } catch (Exception e) {
        log.error("Error verifying leadership for node {}", nodeId, e);
        return false;
    }
}
```

---

## How It Works Now

### Timeline: Healthy Leader with Watchdog

```
Time    Event                                   Leader Node                 Redis Lock
────────────────────────────────────────────────────────────────────────────────────────
T=0s    Leader acquires lock                    ✅ tryAcquireLeadership()   Lock acquired
        Watchdog enabled (leaseTime=-1)         Watchdog started            TTL = 30s

T=3s    Leader verifies leadership              ✅ renewLeadership()        TTL = 30s
        (no manual renewal needed)              Lock still held             (unchanged)

T=6s    Leader verifies leadership              ✅ renewLeadership()        TTL = 30s
        (no manual renewal needed)              Lock still held             (unchanged)

T=9s    Leader verifies leadership              ✅ renewLeadership()        TTL = 30s
        (no manual renewal needed)              Lock still held             (unchanged)

T=10s   ⭐ Watchdog auto-renews lock            Watchdog background task    TTL = 30s ✅
        (happens automatically)                 Lock TTL extended           (renewed!)

T=12s   Leader verifies leadership              ✅ renewLeadership()        TTL = 30s
        (no manual renewal needed)              Lock still held             (unchanged)

T=20s   ⭐ Watchdog auto-renews lock            Watchdog background task    TTL = 30s ✅
        (happens automatically)                 Lock TTL extended           (renewed!)

...     Leader continues indefinitely           ✅ Stable leadership        TTL = 30s
        Watchdog renews every 10s               No leader churn!            (renewed!)
```

### Timeline: Leader Crash with Watchdog

```
Time    Event                                   Leader Node                 Follower Nodes
────────────────────────────────────────────────────────────────────────────────────────
T=0s    Leader holds lock                       ✅ Watchdog active          tryAcquire() → false

T=10s   Watchdog renews lock                    ✅ TTL extended to 30s      tryAcquire() → false

T=15s   💥 LEADER CRASHES                       💀 Node down                tryAcquire() → false
        Watchdog stops                          Watchdog stopped            Lock TTL = 30s

T=18s   Follower attempts acquisition           💀 Node down                tryAcquire() → false
        Lock still held (27s TTL left)                                      Lock still held

T=45s   ⏰ Redis TTL expires                    💀 Node down                🔓 Lock released
        (30s after last watchdog renewal)                                   Lock available!

T=48s   Follower acquires lock                  💀 Node down                ✅ tryAcquire() → TRUE
        New leader elected!                                                 transitionToLeader()
                                                                            ✅ NEW LEADER!
```

---

## Benefits of Watchdog Approach

### ✅ Advantages

1. **Automatic Renewal** - No manual TTL extension logic needed
2. **Robust** - Watchdog handles renewal even if application is busy
3. **Crash Detection** - Lock expires when JVM crashes (watchdog stops)
4. **Simpler Code** - `renewLeadership()` just verifies lock is held
5. **Battle-Tested** - Redisson's watchdog is production-proven

### ⚠️ Considerations

1. **Longer Failover Time** - Default 30s TTL means ~30s failover (vs 10s before)
2. **Configurable** - Can adjust `lockWatchdogTimeout` in Redisson config
3. **Thread-Bound** - Watchdog tied to thread that acquired lock

---

## Configuration (Optional)

To customize watchdog timeout, add to `RedisConfig`:

```java
@Bean(destroyMethod = "shutdown")
public RedissonClient redissonClient() {
    Config config = new Config();
    
    config.useSingleServer()
        .setAddress(address)
        .setPassword(password)
        // ... other settings ...
        .setLockWatchdogTimeout(10000);  // 10 seconds (renewal every 3.3s)
    
    return Redisson.create(config);
}
```

**Recommended**: Keep default 30s for production stability.

---

## Verification

### Build Status
```bash
mvn clean compile
# Result: BUILD SUCCESS ✅
# Time: 11.541 seconds
```

### Expected Behavior
- ✅ Leader holds lock indefinitely while healthy
- ✅ Watchdog renews lock every 10 seconds (default)
- ✅ No unnecessary leader churn
- ✅ Failover occurs only when leader crashes (~30s)

---

## Interview Talking Points

**Q: "How did you fix the leader election bug?"**

**A:** "I identified that the `renewLeadership()` method wasn't actually extending the Redis lock TTL. I fixed it by enabling Redisson's watchdog mechanism, which automatically renews the lock in the background. This eliminated unnecessary leader churn and made the system more robust."

**Q: "What is the watchdog mechanism?"**

**A:** "Redisson's watchdog is an automatic lock renewal feature. When you acquire a lock with leaseTime=-1, Redisson starts a background task that renews the lock every (lockWatchdogTimeout / 3) milliseconds. If the JVM crashes, the watchdog stops and the lock expires naturally, enabling automatic failover."

---

**Fix completed successfully! 🎉**

