# Docker MySQL Connection Issue - Fix Documentation

**Date**: 2026-03-10  
**Issue**: MySQL connection refused in Docker environment  
**Status**: ✅ **RESOLVED**

---

## Problem Summary

**Error**: `java.net.ConnectException: Connection refused`  
**Root Cause**: Configuration mismatch between Docker Compose environment variables and `application-dev.yml`

### What Was Wrong

1. **`application-dev.yml`** had hardcoded database URL: `jdbc:mysql://localhost:3306/scheduler_dev`
2. **Docker Compose** passed `DB_URL` environment variable with `mysql:3306` (service name)
3. **Environment variable was ignored** because the dev profile didn't reference it
4. **Result**: Application tried to connect to `localhost:3306` (container itself) instead of `mysql:3306` (MySQL service)

---

## Solution Applied

### 1. Updated `application-dev.yml` ✅

**Changed database configuration to use environment variables:**

```yaml
# Before (hardcoded)
datasource:
  url: jdbc:mysql://localhost:3306/scheduler_dev?...
  username: root
  password: root

# After (environment variable with default)
datasource:
  url: ${DB_URL:jdbc:mysql://localhost:3306/scheduler_dev?...}
  username: ${DB_USERNAME:root}
  password: ${DB_PASSWORD:root}
```

**Benefits:**
- ✅ Works in Docker (uses `DB_URL=jdbc:mysql://mysql:3306/...`)
- ✅ Works locally (defaults to `localhost:3306`)
- ✅ Consistent with `application-prod.yml` pattern

### 2. Updated Redis Configuration ✅

**Changed Redis configuration to use environment variables:**

```yaml
# Before (hardcoded)
data:
  redis:
    host: localhost
    port: 6379

# After (environment variable with default)
data:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

### 3. Enhanced MySQL Health Check ✅

**Updated `docker-compose.yml` MySQL service:**

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "scheduler_user", "-pscheduler_pass"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s  # Added: Give MySQL 30s to initialize
command: --default-authentication-plugin=mysql_native_password  # Added: Better compatibility
```

### 4. Added Connection Parameters to DB_URL ✅

**Updated all scheduler nodes in `docker-compose.yml`:**

```yaml
DB_URL: jdbc:mysql://mysql:3306/scheduler_dev?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

**Parameters explained:**
- `createDatabaseIfNotExist=true` - Auto-create database if missing
- `useSSL=false` - Disable SSL for local development
- `allowPublicKeyRetrieval=true` - Allow public key retrieval for authentication

---

## Verification Steps

### 1. Clean Up Old Containers

```bash
cd deployment/docker
docker-compose down -v  # Remove containers and volumes
```

### 2. Rebuild and Start Services

```bash
docker-compose up --build -d
```

### 3. Check Service Health

```bash
# Check all services
docker-compose ps

# Expected output:
# scheduler-mysql    healthy
# scheduler-redis    healthy
# scheduler-node-1   running
# scheduler-node-2   running
# scheduler-node-3   running
```

### 4. Verify MySQL Connectivity

```bash
# Test MySQL connection from host
docker exec -it scheduler-mysql mysql -u scheduler_user -pscheduler_pass -e "SHOW DATABASES;"

# Expected output should include: scheduler_dev
```

### 5. Check Application Logs

```bash
# Check node 1 logs
docker logs scheduler-node-1 --tail 50

# Look for successful startup messages:
# - "HikariPool-1 - Start completed"
# - "Flyway migration completed successfully"
# - "Started SchedulerApplication"
```

---

## Testing the Fix

### Test 1: Database Connection

```bash
# Should see Flyway migrations run successfully
docker logs scheduler-node-1 | grep -i flyway
```

### Test 2: Leader Election

```bash
# Check which node became leader
docker logs scheduler-node-1 | grep -i "leader"
docker logs scheduler-node-2 | grep -i "leader"
docker logs scheduler-node-3 | grep -i "leader"
```

### Test 3: Health Check

```bash
# All nodes should return healthy
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

---

## Interview Talking Points

**Q: "How did you debug this Docker networking issue?"**

**A**: "I identified a configuration mismatch between the Docker Compose environment variables and the Spring Boot profile configuration. The key insight was that `localhost` in a Docker container refers to the container itself, not the host machine. I fixed it by:

1. Making the dev profile use environment variables with sensible defaults
2. Ensuring Docker Compose passes the correct service name (`mysql`) instead of `localhost`
3. Adding proper health checks with a startup grace period
4. Including connection parameters for better MySQL compatibility

This demonstrates understanding of Docker networking, Spring Boot externalized configuration, and the importance of environment-specific settings."

---

## Files Modified

1. ✅ `src/main/resources/application-dev.yml` - Added environment variable support
2. ✅ `deployment/docker/docker-compose.yml` - Enhanced MySQL config and DB URLs

---

**Resolution Date**: 2026-03-10  
**Verified By**: Development Team

