@echo off
REM Distributed Job Scheduler - Docker Setup Verification Script (Windows)
REM This script verifies that the Docker environment is correctly configured

echo ==========================================
echo Docker Setup Verification Script
echo ==========================================
echo.

REM Step 1: Check Docker is running
echo Step 1: Checking Docker...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker and try again.
    exit /b 1
)
echo [OK] Docker is running

REM Step 2: Check Docker Compose is available
echo.
echo Step 2: Checking Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose is not available. Please install Docker Compose.
    exit /b 1
)
echo [OK] Docker Compose is available

REM Step 3: Clean up old containers
echo.
echo Step 3: Cleaning up old containers...
docker-compose down -v >nul 2>&1
echo [OK] Old containers removed

REM Step 4: Build and start services
echo.
echo Step 4: Building and starting services...
echo This may take a few minutes on first run...
docker-compose up --build -d

REM Step 5: Wait for MySQL to be healthy
echo.
echo Step 5: Waiting for MySQL to be healthy...
timeout /t 30 /nobreak >nul
echo [OK] Waiting complete

REM Step 6: Check service status
echo.
echo Step 6: Service Status
echo ==========================================
docker-compose ps

REM Step 7: Test MySQL connection
echo.
echo Step 7: Testing MySQL connection...
docker exec scheduler-mysql mysql -u scheduler_user -pscheduler_pass -e "SHOW DATABASES;" 2>nul | find "scheduler_dev" >nul
if %errorlevel% equ 0 (
    echo [OK] MySQL database 'scheduler_dev' exists
) else (
    echo [WARNING] MySQL database 'scheduler_dev' not found yet
)

REM Step 8: Check scheduler node 1 logs
echo.
echo Step 8: Checking scheduler node 1 logs...
echo Last 30 lines of scheduler-node-1:
echo ==========================================
docker logs scheduler-node-1 --tail 30

REM Final summary
echo.
echo ==========================================
echo Verification Complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Check logs: docker logs scheduler-node-1 --tail 50
echo 2. Test API: curl http://localhost:8080/actuator/health
echo 3. View all logs: docker-compose logs -f
echo.
echo To stop services: docker-compose down
echo To stop and remove volumes: docker-compose down -v
echo.
pause

