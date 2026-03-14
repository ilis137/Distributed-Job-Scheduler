# Docker Troubleshooting Guide

**Last Updated**: 2026-03-10  
**Project**: Distributed Job Scheduler

---

## Common Issues and Solutions

### 1. MySQL Connection Refused ✅ FIXED

**Symptom:**
```
java.net.ConnectException: Connection refused
HikariPool-1 - Exception during pool initialization
```

**Root Cause:**
- `application-dev.yml` had hardcoded `localhost:3306`
- Docker containers use service names (e.g., `mysql`) for networking
- Environment variables were being ignored

**Solution:**
- Updated `application-dev.yml` to use `${DB_URL:...}` pattern
- Updated `docker-compose.yml` with correct service names
- See: `docs/DOCKER_MYSQL_CONNECTION_FIX.md`

---

### 2. MySQL Not Ready During Startup

**Symptom:**
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago
```

**Root Cause:**
- Scheduler starts before MySQL is fully initialized
- Health check passes but MySQL still initializing

**Solution:**
```yaml
# In docker-compose.yml
healthcheck:
  start_period: 30s  # Give MySQL 30s to initialize
depends_on:
  mysql:
    condition: service_healthy  # Wait for health check
```

---

### 3. Flyway Migration Failures

**Symptom:**
```
Flyway migration failed
Table 'scheduler_dev.flyway_schema_history' doesn't exist
```

**Solutions:**

**Option 1: Clean Start**
```bash
docker-compose down -v  # Remove volumes
docker-compose up --build -d
```

**Option 2: Manual Database Reset**
```bash
docker exec -it scheduler-mysql mysql -u root -prootpass
DROP DATABASE scheduler_dev;
CREATE DATABASE scheduler_dev;
exit
docker-compose restart scheduler-node-1
```

---

### 4. Port Already in Use

**Symptom:**
```
Error starting userland proxy: listen tcp 0.0.0.0:3306: bind: address already in use
```

**Solution:**

**Option 1: Stop local MySQL**
```bash
# Windows
net stop MySQL80

# Linux/Mac
sudo systemctl stop mysql
```

**Option 2: Change Docker port mapping**
```yaml
# In docker-compose.yml
ports:
  - "3307:3306"  # Map to different host port
```

---

### 5. Redis Connection Issues

**Symptom:**
```
Unable to connect to Redis
RedisConnectionException
```

**Solution:**
```bash
# Check Redis is running
docker logs scheduler-redis

# Test Redis connection
docker exec -it scheduler-redis redis-cli ping
# Should return: PONG

# Restart Redis
docker-compose restart redis
```

---

### 6. Application Won't Start

**Symptom:**
- Container exits immediately
- No logs visible

**Debugging Steps:**

```bash
# 1. Check container status
docker-compose ps

# 2. View full logs
docker logs scheduler-node-1

# 3. Check for build errors
docker-compose up --build

# 4. Run container interactively
docker run -it --rm scheduler-node-1 /bin/sh
```

---

### 7. Network Issues Between Containers

**Symptom:**
```
UnknownHostException: mysql
Name or service not known
```

**Solution:**
```bash
# 1. Verify network exists
docker network ls | grep scheduler

# 2. Inspect network
docker network inspect scheduler-network

# 3. Recreate network
docker-compose down
docker network prune
docker-compose up -d
```

---

## Diagnostic Commands

### Check Service Health
```bash
# All services
docker-compose ps

# Specific service
docker inspect scheduler-mysql --format='{{.State.Health.Status}}'
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker logs scheduler-node-1 --tail 100 -f

# Search logs
docker logs scheduler-node-1 | grep -i error
```

### Test Database Connection
```bash
# From host
docker exec -it scheduler-mysql mysql -u scheduler_user -pscheduler_pass

# Show databases
docker exec scheduler-mysql mysql -u scheduler_user -pscheduler_pass -e "SHOW DATABASES;"

# Show tables
docker exec scheduler-mysql mysql -u scheduler_user -pscheduler_pass scheduler_dev -e "SHOW TABLES;"
```

### Test Redis Connection
```bash
# Ping Redis
docker exec scheduler-redis redis-cli ping

# Check keys
docker exec scheduler-redis redis-cli keys '*'

# Monitor commands
docker exec scheduler-redis redis-cli monitor
```

### Test Application Endpoints
```bash
# Health check
curl http://localhost:8080/actuator/health

# Info endpoint
curl http://localhost:8080/actuator/info

# All nodes
for port in 8080 8081 8082; do
  echo "Node on port $port:"
  curl -s http://localhost:$port/actuator/health | jq .
done
```

---

## Clean Slate Procedure

If all else fails, start fresh:

```bash
# 1. Stop all containers
docker-compose down -v

# 2. Remove all scheduler images
docker images | grep scheduler | awk '{print $3}' | xargs docker rmi -f

# 3. Prune Docker system
docker system prune -a --volumes

# 4. Rebuild from scratch
docker-compose up --build -d

# 5. Watch logs
docker-compose logs -f
```

---

## Verification Checklist

Use the automated verification script:

```bash
# Linux/Mac
cd deployment/docker
chmod +x verify-docker-setup.sh
./verify-docker-setup.sh

# Windows
cd deployment\docker
verify-docker-setup.bat
```

Manual verification:

- [ ] Docker is running
- [ ] Docker Compose is available
- [ ] MySQL container is healthy
- [ ] Redis container is healthy
- [ ] Scheduler nodes are running
- [ ] Database `scheduler_dev` exists
- [ ] Flyway migrations completed
- [ ] Health endpoints respond
- [ ] Leader election occurred

---

## Getting Help

If issues persist:

1. **Collect diagnostic information:**
   ```bash
   docker-compose ps > docker-status.txt
   docker-compose logs > docker-logs.txt
   docker network inspect scheduler-network > network-info.txt
   ```

2. **Check documentation:**
   - `docs/DOCKER_MYSQL_CONNECTION_FIX.md`
   - `DEVELOPMENT.md`
   - `README.md`

3. **Review configuration:**
   - `deployment/docker/docker-compose.yml`
   - `src/main/resources/application-dev.yml`
   - `deployment/docker/Dockerfile`

---

**Last Updated**: 2026-03-10

