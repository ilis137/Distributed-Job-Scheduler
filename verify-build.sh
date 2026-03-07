#!/bin/bash

# Distributed Job Scheduler - Build Verification Script
# This script verifies that the build environment is correctly configured

echo "=========================================="
echo "Build Environment Verification"
echo "=========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java version
echo "1. Checking Java version..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓ Java $JAVA_VERSION installed${NC}"
    else
        echo -e "${RED}✗ Java 21 or higher required (found Java $JAVA_VERSION)${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Java not found${NC}"
    exit 1
fi

# Check Maven version
echo "2. Checking Maven version..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1 | awk '{print $3}')
    echo -e "${GREEN}✓ Maven $MVN_VERSION installed${NC}"
else
    echo -e "${RED}✗ Maven not found${NC}"
    exit 1
fi

# Check JAVA_HOME
echo "3. Checking JAVA_HOME..."
if [ -n "$JAVA_HOME" ]; then
    echo -e "${GREEN}✓ JAVA_HOME set to: $JAVA_HOME${NC}"
else
    echo -e "${YELLOW}⚠ JAVA_HOME not set (may cause issues)${NC}"
fi

# Check Flyway migrations
echo "4. Checking Flyway migrations..."
if [ -d "src/main/resources/db/migration" ] && [ "$(ls -A src/main/resources/db/migration)" ]; then
    echo -e "${GREEN}✓ Flyway migrations exist${NC}"
else
    echo -e "${RED}✗ Flyway migrations missing${NC}"
    exit 1
fi

# Check Docker (optional)
echo "5. Checking Docker (optional for tests)..."
if command -v docker &> /dev/null; then
    if docker ps &> /dev/null; then
        echo -e "${GREEN}✓ Docker is running${NC}"
    else
        echo -e "${YELLOW}⚠ Docker installed but not running${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Docker not found (tests will be skipped)${NC}"
fi

# Check port 8080
echo "6. Checking if port 8080 is available..."
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Port 8080 is in use${NC}"
else
    echo -e "${GREEN}✓ Port 8080 is available${NC}"
fi

echo ""
echo "=========================================="
echo "Build Recommendations"
echo "=========================================="
echo ""

# Provide build recommendations
if ! command -v docker &> /dev/null || ! docker ps &> /dev/null; then
    echo -e "${YELLOW}Docker not available. Recommended build command:${NC}"
    echo "  mvn clean install -DskipTests"
else
    echo -e "${GREEN}Docker available. Recommended build command:${NC}"
    echo "  mvn clean install"
fi

echo ""
echo "Alternative build commands:"
echo "  mvn clean install -DskipTests           # Skip all tests"
echo "  mvn clean install -Djacoco.skip=true    # Skip coverage check"
echo "  mvn clean package                       # Build without install"
echo ""

# Try a quick validation
echo "=========================================="
echo "Running Maven Validation"
echo "=========================================="
echo ""

mvn validate

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Maven validation successful${NC}"
    echo ""
    echo "You can now run: mvn clean install"
else
    echo -e "${RED}✗ Maven validation failed${NC}"
    echo "Check the errors above and fix before building"
    exit 1
fi

