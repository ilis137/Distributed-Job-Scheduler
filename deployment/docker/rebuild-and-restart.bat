@echo off
REM Rebuild Docker images and restart all services
REM This script ensures the latest code changes are included in the Docker images

echo ==========================================
echo Rebuilding Docker Images and Restarting Services
echo ==========================================
echo.

echo Step 1: Stopping all containers...
docker-compose down
echo.

echo Step 2: Removing old images to force rebuild...
docker rmi docker-scheduler-node-1:latest docker-scheduler-node-2:latest docker-scheduler-node-3:latest 2>nul
echo.

echo Step 3: Building new images with latest code...
docker-compose build --no-cache
echo.

echo Step 4: Starting all services...
docker-compose up -d
echo.

echo Step 5: Waiting for services to start (30 seconds)...
timeout /t 30 /nobreak >nul
echo.

echo Step 6: Checking service status...
docker-compose ps
echo.

echo Step 7: Checking scheduler-node-1 logs...
echo ==========================================
docker logs scheduler-node-1 --tail 30
echo.

echo ==========================================
echo Rebuild Complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Check logs: docker logs scheduler-node-1 -f
echo 2. Test health: curl http://localhost:8080/actuator/health
echo 3. View all logs: docker-compose logs -f
echo.
pause

