#!/bin/bash

# Distributed Job Scheduler - Docker Setup Verification Script
# This script verifies that the Docker environment is correctly configured

set -e

echo "=========================================="
echo "Docker Setup Verification Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print success
success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Function to print error
error() {
    echo -e "${RED}✗${NC} $1"
}

# Function to print warning
warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Step 1: Check Docker is running
echo "Step 1: Checking Docker..."
if docker info > /dev/null 2>&1; then
    success "Docker is running"
else
    error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Step 2: Check Docker Compose is available
echo ""
echo "Step 2: Checking Docker Compose..."
if docker-compose --version > /dev/null 2>&1; then
    success "Docker Compose is available"
else
    error "Docker Compose is not available. Please install Docker Compose."
    exit 1
fi

# Step 3: Clean up old containers
echo ""
echo "Step 3: Cleaning up old containers..."
docker-compose down -v > /dev/null 2>&1 || true
success "Old containers removed"

# Step 4: Build and start services
echo ""
echo "Step 4: Building and starting services..."
echo "This may take a few minutes on first run..."
docker-compose up --build -d

# Step 5: Wait for MySQL to be healthy
echo ""
echo "Step 5: Waiting for MySQL to be healthy..."
MAX_WAIT=60
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker inspect scheduler-mysql --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; then
        success "MySQL is healthy"
        break
    fi
    echo -n "."
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    error "MySQL failed to become healthy within ${MAX_WAIT} seconds"
    echo "Checking MySQL logs:"
    docker logs scheduler-mysql --tail 20
    exit 1
fi

# Step 6: Wait for Redis to be healthy
echo ""
echo "Step 6: Waiting for Redis to be healthy..."
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker inspect scheduler-redis --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; then
        success "Redis is healthy"
        break
    fi
    echo -n "."
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
done

if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
    error "Redis failed to become healthy within ${MAX_WAIT} seconds"
    exit 1
fi

# Step 7: Wait for scheduler nodes to start
echo ""
echo "Step 7: Waiting for scheduler nodes to start..."
sleep 10

# Step 8: Check scheduler node 1
echo ""
echo "Step 8: Checking scheduler node 1..."
if docker logs scheduler-node-1 2>&1 | grep -q "Started SchedulerApplication"; then
    success "Scheduler node 1 started successfully"
else
    warning "Scheduler node 1 may still be starting. Checking logs..."
    docker logs scheduler-node-1 --tail 30
fi

# Step 9: Test MySQL connection
echo ""
echo "Step 9: Testing MySQL connection..."
if docker exec scheduler-mysql mysql -u scheduler_user -pscheduler_pass -e "SHOW DATABASES;" 2>/dev/null | grep -q "scheduler_dev"; then
    success "MySQL database 'scheduler_dev' exists"
else
    error "MySQL database 'scheduler_dev' not found"
    exit 1
fi

# Step 10: Check Flyway migrations
echo ""
echo "Step 10: Checking Flyway migrations..."
if docker logs scheduler-node-1 2>&1 | grep -q "Flyway"; then
    success "Flyway migrations executed"
else
    warning "Flyway migrations may not have run yet"
fi

# Step 11: Test health endpoints
echo ""
echo "Step 11: Testing health endpoints..."
sleep 5
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    success "Node 1 health endpoint is accessible"
else
    warning "Node 1 health endpoint not yet accessible (may still be starting)"
fi

# Step 12: Show service status
echo ""
echo "Step 12: Service Status"
echo "=========================================="
docker-compose ps

# Final summary
echo ""
echo "=========================================="
echo "Verification Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Check logs: docker logs scheduler-node-1 --tail 50"
echo "2. Test API: curl http://localhost:8080/actuator/health"
echo "3. View all logs: docker-compose logs -f"
echo ""
echo "To stop services: docker-compose down"
echo "To stop and remove volumes: docker-compose down -v"
echo ""

