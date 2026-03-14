# Docker Quick Start Guide

**Project**: Distributed Job Scheduler  
**Last Updated**: 2026-03-10

---

## Prerequisites

- Docker Desktop installed and running
- Docker Compose available
- At least 4GB RAM allocated to Docker
- Ports available: 3306, 6379, 8080, 8081, 8082

---

## Quick Start (3 Steps)

### 1. Navigate to Docker Directory

```bash
cd deployment/docker
```

### 2. Start All Services

```bash
# Clean start (recommended first time)
docker-compose down -v
docker-compose up --build -d
```

### 3. Verify Services

```bash
# Automated verification (Linux/Mac)
./verify-docker-setup.sh

# Automated verification (Windows)
verify-docker-setup.bat

# Manual verification
docker-compose ps
```

---

## Expected Output

```
NAME                STATUS              PORTS
scheduler-mysql     Up (healthy)        0.0.0.0:3306->3306/tcp
scheduler-redis     Up (healthy)        0.0.0.0:6379->6379/tcp
scheduler-node-1    Up                  0.0.0.0:8080->8080/tcp
scheduler-node-2    Up                  0.0.0.0:8081->8081/tcp
scheduler-node-3    Up                  0.0.0.0:8082->8082/tcp
```

---

## Test the Application

### Health Checks

```bash
# Node 1
curl http://localhost:8080/actuator/health

# Node 2
curl http://localhost:8081/actuator/health

# Node 3
curl http://localhost:8082/actuator/health
```

### Check Leader Election

```bash
# View logs to see which node became leader
docker logs scheduler-node-1 | grep -i "leader"
docker logs scheduler-node-2 | grep -i "leader"
docker logs scheduler-node-3 | grep -i "leader"
```

### Test Database

```bash
# Connect to MySQL
docker exec -it scheduler-mysql mysql -u scheduler_user -pscheduler_pass

# Show databases
SHOW DATABASES;

# Use scheduler database
USE scheduler_dev;

# Show tables (should see Flyway migrations)
SHOW TABLES;

# Exit
exit
```

---

## Common Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker logs scheduler-node-1 -f

# Last 50 lines
docker logs scheduler-node-1 --tail 50
```

### Restart Services

```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart scheduler-node-1
```

### Stop Services

```bash
# Stop (keep data)
docker-compose stop

# Stop and remove containers (keep data)
docker-compose down

# Stop and remove everything (clean slate)
docker-compose down -v
```

### Rebuild After Code Changes

```bash
# Rebuild and restart
docker-compose up --build -d

# Rebuild specific service
docker-compose up --build -d scheduler-node-1
```

---

## Troubleshooting

### Issue: Connection Refused

**Solution**: Ensure MySQL is healthy before scheduler starts

```bash
docker-compose down -v
docker-compose up -d mysql redis
# Wait 30 seconds
docker-compose up -d scheduler-node-1 scheduler-node-2 scheduler-node-3
```

### Issue: Port Already in Use

**Solution**: Stop local MySQL/Redis or change ports

```bash
# Option 1: Stop local services
# Windows: net stop MySQL80
# Linux: sudo systemctl stop mysql

# Option 2: Edit docker-compose.yml ports
# Change "3306:3306" to "3307:3306"
```

### Issue: Flyway Migration Failed

**Solution**: Reset database

```bash
docker-compose down -v
docker-compose up --build -d
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                Load Balancer                    │
│         (localhost:8080/8081/8082)              │
└────────────┬────────────┬────────────┬──────────┘
             │            │            │
    ┌────────▼───┐  ┌────▼─────┐  ┌──▼──────┐
    │  Node 1    │  │  Node 2  │  │  Node 3 │
    │  (Leader)  │  │(Follower)│  │(Follower)│
    └────────┬───┘  └────┬─────┘  └──┬──────┘
             │            │            │
             └────────────┼────────────┘
                          │
              ┌───────────▼───────────┐
              │                       │
         ┌────▼─────┐          ┌─────▼────┐
         │  MySQL   │          │  Redis   │
         │  :3306   │          │  :6379   │
         └──────────┘          └──────────┘
```

---

## Next Steps

1. **Test Job Creation**: Use the REST API to create jobs
2. **Monitor Leader Election**: Watch logs during node failures
3. **Test Failover**: Stop leader node and watch follower take over
4. **Load Testing**: Submit multiple jobs concurrently

---

## Additional Resources

- **Full Documentation**: `../../docs/DOCKER_MYSQL_CONNECTION_FIX.md`
- **Troubleshooting**: `../../docs/DOCKER_TROUBLESHOOTING_GUIDE.md`
- **Development Guide**: `../../DEVELOPMENT.md`
- **Architecture**: `../../ARCHITECTURE.md`

---

**Need Help?** Check `docs/DOCKER_TROUBLESHOOTING_GUIDE.md`

