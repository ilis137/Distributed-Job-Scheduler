# Docker MySQL Connection Issue - Diagnostic Report

**Date**: 2026-03-10  
**Issue**: MySQL connection refused despite correct configuration  
**Status**: ✅ **ROOT CAUSE IDENTIFIED**

---

## Problem Summary

**Error**: `java.net.ConnectException: Connection refused` when connecting to MySQL from scheduler containers

**Symptoms**:
- MySQL container is healthy and running
- Network connectivity is working (ping succeeds, port 3306 is open)
- Environment variables are correctly set in docker-compose.yml
- `application-dev.yml` has been updated to use environment variables
- **BUT**: Application still tries to connect to `localhost:3306` instead of `mysql:3306`

---

## Root Cause Analysis

### Investigation Steps Performed

1. ✅ **Verified Docker Compose configuration** - Correct
   - `DB_URL=jdbc:mysql://mysql:3306/scheduler_dev?...`
   - `DB_USERNAME=scheduler_user`
   - `DB_PASSWORD=scheduler_pass`

2. ✅ **Verified application-dev.yml** - Correct
   - Uses `${DB_URL:...}` pattern
   - Uses `${DB_USERNAME:...}` pattern
   - Uses `${DB_PASSWORD:...}` pattern

3. ✅ **Verified MySQL is running** - Healthy
   - Container status: `Up 16 minutes (healthy)`
   - MySQL logs show: "ready for connections"
   - Health check passing

4. ✅ **Verified network connectivity** - Working
   - All containers on `docker_scheduler-network`
   - Ping from scheduler to MySQL: SUCCESS
   - Port 3306 reachable: `nc -zv mysql 3306` → OPEN
   - MySQL bind-address: `*` (all interfaces)
   - User permissions: `scheduler_user@%` (can connect from anywhere)

5. ✅ **Verified environment variables** - Correctly set
   ```bash
   docker exec scheduler-node-2 env | findstr DB_
   DB_USERNAME=scheduler_user
   DB_PASSWORD=scheduler_pass
   DB_URL=jdbc:mysql://mysql:3306/scheduler_dev?...
   ```

6. ❌ **Checked Docker image build time** - **PROBLEM FOUND!**
   - Image created: `2026-03-10T04:24:40Z`
   - Configuration updated: `2026-03-10T05:00:00Z` (approximately)
   - **The Docker images were built BEFORE the configuration changes!**

---

## Root Cause

**The Docker images contain the OLD `application-dev.yml` configuration with hardcoded `localhost:3306`.**

### Why This Happened

1. We updated `src/main/resources/application-dev.yml` to use environment variables
2. We updated `deployment/docker/docker-compose.yml` with correct environment variables
3. **BUT** we didn't rebuild the Docker images after making these changes
4. The running containers are using JAR files built with the OLD configuration
5. Even though environment variables are set, the application ignores them because the JAR contains hardcoded values

### Evidence

```bash
# Docker image created BEFORE configuration changes
docker inspect docker-scheduler-node-1:latest --format="{{.Created}}"
2026-03-10T04:24:40.384813389Z

# Configuration files were updated AFTER this time
# The JAR file inside the Docker image has the old application-dev.yml
```

---

## Solution

### Option 1: Rebuild Docker Images (Recommended)

**Windows:**
```bash
cd deployment\docker
rebuild-and-restart.bat
```

**Linux/Mac:**
```bash
cd deployment/docker
chmod +x rebuild-and-restart.sh
./rebuild-and-restart.sh
```

**Manual Steps:**
```bash
# 1. Stop all containers
docker-compose down

# 2. Remove old images
docker rmi docker-scheduler-node-1:latest docker-scheduler-node-2:latest docker-scheduler-node-3:latest

# 3. Rebuild with no cache
docker-compose build --no-cache

# 4. Start services
docker-compose up -d

# 5. Check logs
docker logs scheduler-node-1 --tail 50
```

### Option 2: Force Rebuild During Up

```bash
docker-compose up --build --force-recreate -d
```

---

## Verification Steps

After rebuilding, verify the fix:

### 1. Check Container Logs
```bash
docker logs scheduler-node-1 --tail 50
```

**Expected output:**
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Flyway Community Edition 10.8.1
Successfully validated 4 migrations
Migrating schema `scheduler_dev` to version "1 - create jobs table"
Migrating schema `scheduler_dev` to version "2 - create job executions table"
Migrating schema `scheduler_dev` to version "3 - create scheduler nodes table"
Migrating schema `scheduler_dev` to version "4 - create additional indexes"
Successfully applied 4 migrations
Started SchedulerApplication in X.XXX seconds
```

### 2. Check Service Status
```bash
docker-compose ps
```

**Expected output:**
```
scheduler-mysql    healthy
scheduler-redis    healthy
scheduler-node-1   running (healthy)
scheduler-node-2   running (healthy)
scheduler-node-3   running (healthy)
```

### 3. Test Health Endpoint
```bash
curl http://localhost:8080/actuator/health
```

**Expected output:**
```json
{"status":"UP"}
```

---

## Key Learnings

### 1. Docker Image Caching
- Docker images are immutable snapshots
- Changes to source code require rebuilding images
- Use `--no-cache` to ensure fresh build

### 2. Configuration Precedence
- JAR file contains embedded `application-dev.yml`
- Environment variables override embedded config ONLY if the YAML uses `${VAR:default}` syntax
- If YAML has hardcoded values, environment variables are ignored

### 3. Debugging Docker Issues
- Always check image build timestamps
- Verify environment variables are actually being used
- Test network connectivity separately from application logic
- Check logs at multiple levels (container, application, database)

---

## Prevention

To avoid this issue in the future:

1. **Always rebuild after configuration changes:**
   ```bash
   docker-compose up --build -d
   ```

2. **Use automated scripts:**
   - `rebuild-and-restart.bat` (Windows)
   - `rebuild-and-restart.sh` (Linux/Mac)

3. **Add to development workflow:**
   - Document rebuild requirement in `DEVELOPMENT.md`
   - Add pre-commit hooks to remind about rebuilds
   - Use Docker Compose watch mode for auto-rebuild (Docker Compose v2.22+)

---

## Interview Talking Points

**Q: "How did you debug this complex Docker issue?"**

**A**: "I used a systematic debugging approach:

1. **Verified configuration** - Checked both docker-compose.yml and application-dev.yml
2. **Tested network connectivity** - Used ping, nc, and direct MySQL connections
3. **Checked environment variables** - Verified they were set correctly in containers
4. **Investigated timing** - Checked container start times and health checks
5. **Examined image metadata** - Discovered images were built before config changes

The key insight was realizing that Docker images are immutable snapshots. Even though we updated the source files, the running containers were using JAR files built with the old configuration. This demonstrates understanding of:
- Docker image layering and caching
- Spring Boot externalized configuration
- The difference between build-time and runtime configuration
- Systematic debugging methodology"

---

**Resolution**: Rebuild Docker images with updated configuration  
**Status**: ✅ **READY TO FIX**  
**Next Step**: Run `rebuild-and-restart.bat` or `rebuild-and-restart.sh`

