@echo off
REM Leader Election Verification Script (Windows)
REM Tests the leader election fix after applying changes to RedisCoordinationService.java

echo ==========================================
echo Leader Election Verification Script
echo ==========================================
echo.

REM Step 1: Check Redis lock TTL
echo Step 1: Checking Redis lock TTL...
docker exec redis redis-cli TTL scheduler:leader:election > temp_ttl.txt 2>nul
set /p TTL=<temp_ttl.txt
del temp_ttl.txt 2>nul

if "%TTL%"=="-2" (
    echo [ERROR] Redis lock does not exist ^(no leader elected yet^)
    echo    This is normal if nodes just started. Wait 5 seconds and try again.
) else if "%TTL%"=="-1" (
    echo [ERROR] Redis lock has infinite TTL ^(WATCHDOG STILL ENABLED - BUG NOT FIXED!^)
    echo    Expected: TTL between 1-10 seconds
    echo    Actual: TTL = -1 ^(never expires^)
    echo.
    echo    ACTION REQUIRED: Rebuild Docker image with the fix
    pause
    exit /b 1
) else (
    echo [OK] Redis lock has TTL: %TTL% seconds
)
echo.

REM Step 2: Check database for leader
echo Step 2: Checking database for leader...
docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -sN -e "SELECT COUNT(*) FROM scheduler_nodes WHERE role='LEADER';" > temp_count.txt 2>nul
set /p LEADER_COUNT=<temp_count.txt
del temp_count.txt 2>nul

if "%LEADER_COUNT%"=="1" (
    docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -sN -e "SELECT node_id FROM scheduler_nodes WHERE role='LEADER';" > temp_leader.txt 2>nul
    set /p LEADER_NODE=<temp_leader.txt
    del temp_leader.txt 2>nul
    echo [OK] Exactly 1 leader found in database: %LEADER_NODE%
) else if "%LEADER_COUNT%"=="0" (
    echo [ERROR] No leader found in database
    echo    This may indicate a race condition or startup delay
    echo    Wait 10 seconds and run this script again
) else (
    echo [ERROR] Multiple leaders found in database: %LEADER_COUNT% ^(SPLIT-BRAIN!^)
    echo    This is a critical issue - restart all nodes
)
echo.

REM Step 3: Check all nodes status
echo Step 3: Checking all nodes status...
docker exec mysql mysql -u scheduler -pscheduler123 scheduler_db -e "SELECT node_id, role, epoch, healthy, last_heartbeat FROM scheduler_nodes ORDER BY role DESC, last_heartbeat DESC;" 2>nul
echo.

REM Step 4: Check application logs
echo Step 4: Checking application logs for leader election...
echo.
echo Node 1 logs:
docker logs scheduler-node-1 --tail 5 2>nul | findstr /I "leadership"
if errorlevel 1 echo   ^(no leadership messages^)
echo.
echo Node 2 logs:
docker logs scheduler-node-2 --tail 5 2>nul | findstr /I "leadership"
if errorlevel 1 echo   ^(no leadership messages^)
echo.
echo Node 3 logs:
docker logs scheduler-node-3 --tail 5 2>nul | findstr /I "leadership"
if errorlevel 1 echo   ^(no leadership messages^)
echo.

REM Step 5: Test API endpoint
echo Step 5: Testing cluster status API...
curl -s http://localhost:8080/api/v1/cluster/status > temp_api.txt 2>nul
findstr /C:"leaderNodeId" temp_api.txt >nul
if errorlevel 1 (
    echo [ERROR] API does not report a leader
) else (
    echo [OK] API reports a leader
)
del temp_api.txt 2>nul
echo.

REM Summary
echo ==========================================
echo Summary
echo ==========================================
echo.

if "%LEADER_COUNT%"=="1" (
    if not "%TTL%"=="-1" (
        echo [SUCCESS] Leader election is working correctly!
        echo.
        echo Next steps:
        echo   1. Test leader failover: docker stop scheduler-node-1
        echo   2. Wait 12 seconds for new election
        echo   3. Run this script again to verify new leader
    )
) else if "%TTL%"=="-1" (
    echo [ERROR] Fix not applied - watchdog still enabled
    echo.
    echo Action required:
    echo   1. Verify changes in RedisCoordinationService.java
    echo   2. Rebuild: docker-compose build scheduler-node-1
    echo   3. Restart: docker-compose down ^&^& docker-compose up -d
    echo   4. Run this script again
) else (
    echo [INFO] Leader election may still be initializing
    echo.
    echo Wait 10 seconds and run this script again:
    echo   deployment\docker\verify-leader-election.bat
)
echo.

pause

