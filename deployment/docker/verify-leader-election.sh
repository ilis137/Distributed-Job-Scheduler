#!/bin/bash

# Leader Election Verification Script
# Tests the leader election fix after applying changes to RedisCoordinationService.java

set -e

echo "=========================================="
echo "Leader Election Verification Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Step 1: Check Redis lock TTL
echo "Step 1: Checking Redis lock TTL..."
TTL=$(docker exec redis redis-cli TTL scheduler:leader:election 2>/dev/null || echo "-2")

if [ "$TTL" == "-2" ]; then
    print_error "Redis lock does not exist (no leader elected yet)"
    echo "   This is normal if nodes just started. Wait 5 seconds and try again."
elif [ "$TTL" == "-1" ]; then
    print_error "Redis lock has infinite TTL (WATCHDOG STILL ENABLED - BUG NOT FIXED!)"
    echo "   Expected: TTL between 1-10 seconds"
    echo "   Actual: TTL = -1 (never expires)"
    echo ""
    echo "   ACTION REQUIRED: Rebuild Docker image with the fix"
    exit 1
elif [ "$TTL" -ge 1 ] && [ "$TTL" -le 10 ]; then
    print_success "Redis lock has correct TTL: ${TTL} seconds"
else
    print_info "Redis lock TTL: ${TTL} seconds (unusual but may be valid)"
fi
echo ""

# Step 2: Check database for leader
echo "Step 2: Checking database for leader..."
LEADER_COUNT=$(docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -sN \
    -e "SELECT COUNT(*) FROM scheduler_nodes WHERE role='LEADER';" 2>/dev/null || echo "0")

if [ "$LEADER_COUNT" == "1" ]; then
    LEADER_NODE=$(docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -sN \
        -e "SELECT node_id FROM scheduler_nodes WHERE role='LEADER';" 2>/dev/null)
    print_success "Exactly 1 leader found in database: ${LEADER_NODE}"
elif [ "$LEADER_COUNT" == "0" ]; then
    print_error "No leader found in database"
    echo "   This may indicate a race condition or startup delay"
    echo "   Wait 10 seconds and run this script again"
else
    print_error "Multiple leaders found in database: ${LEADER_COUNT} (SPLIT-BRAIN!)"
    echo "   This is a critical issue - restart all nodes"
fi
echo ""

# Step 3: Check all nodes status
echo "Step 3: Checking all nodes status..."
docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -e \
    "SELECT node_id, role, epoch, healthy, last_heartbeat FROM scheduler_nodes ORDER BY role DESC, last_heartbeat DESC;" \
    2>/dev/null || print_error "Failed to query database"
echo ""

# Step 4: Check application logs for leader election
echo "Step 4: Checking application logs for leader election..."
echo ""
echo "Node 1 logs:"
docker logs scheduler-node-1 --tail 5 2>/dev/null | grep -i "leadership" || echo "  (no leadership messages)"
echo ""
echo "Node 2 logs:"
docker logs scheduler-node-2 --tail 5 2>/dev/null | grep -i "leadership" || echo "  (no leadership messages)"
echo ""
echo "Node 3 logs:"
docker logs scheduler-node-3 --tail 5 2>/dev/null | grep -i "leadership" || echo "  (no leadership messages)"
echo ""

# Step 5: Test API endpoint
echo "Step 5: Testing cluster status API..."
RESPONSE=$(curl -s http://localhost:8080/api/v1/cluster/status 2>/dev/null || echo "{}")
LEADER_ID=$(echo "$RESPONSE" | grep -o '"leaderNodeId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$LEADER_ID" ]; then
    print_success "API reports leader: ${LEADER_ID}"
else
    print_error "API does not report a leader"
fi
echo ""

# Summary
echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""

if [ "$TTL" -ge 1 ] && [ "$TTL" -le 10 ] && [ "$LEADER_COUNT" == "1" ]; then
    print_success "Leader election is working correctly!"
    echo ""
    echo "Next steps:"
    echo "  1. Test leader failover: docker stop scheduler-node-1"
    echo "  2. Wait 12 seconds for new election"
    echo "  3. Run this script again to verify new leader"
elif [ "$TTL" == "-1" ]; then
    print_error "Fix not applied - watchdog still enabled"
    echo ""
    echo "Action required:"
    echo "  1. Verify changes in RedisCoordinationService.java"
    echo "  2. Rebuild: docker-compose build scheduler-node-1"
    echo "  3. Restart: docker-compose down && docker-compose up -d"
    echo "  4. Run this script again"
else
    print_info "Leader election may still be initializing"
    echo ""
    echo "Wait 10 seconds and run this script again:"
    echo "  ./deployment/docker/verify-leader-election.sh"
fi
echo ""

