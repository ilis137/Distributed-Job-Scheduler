@echo off
REM Distributed Job Scheduler - Build Verification Script (Windows)
REM This script verifies that the build environment is correctly configured

echo ==========================================
echo Build Environment Verification
echo ==========================================
echo.

REM Check Java version
echo 1. Checking Java version...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    echo Please install Java 21 or higher
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo [OK] Java version: %JAVA_VERSION%

REM Check Maven version
echo 2. Checking Maven version...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found
    echo Please install Maven 3.9 or higher
    exit /b 1
)

for /f "tokens=3" %%g in ('mvn -version ^| findstr /i "Apache Maven"') do (
    set MVN_VERSION=%%g
)
echo [OK] Maven version: %MVN_VERSION%

REM Check JAVA_HOME
echo 3. Checking JAVA_HOME...
if defined JAVA_HOME (
    echo [OK] JAVA_HOME set to: %JAVA_HOME%
) else (
    echo [WARNING] JAVA_HOME not set
)

REM Check Flyway migrations
echo 4. Checking Flyway migrations...
if exist "src\main\resources\db\migration" (
    dir /b "src\main\resources\db\migration\*.sql" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Flyway migrations exist
    ) else (
        echo [ERROR] Flyway migrations directory is empty
        exit /b 1
    )
) else (
    echo [ERROR] Flyway migrations directory missing
    echo Expected: src\main\resources\db\migration
    exit /b 1
)

REM Check Docker
echo 5. Checking Docker (optional for tests)...
docker ps >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Docker is running
) else (
    echo [WARNING] Docker not running (tests will be skipped)
)

REM Check port 8080
echo 6. Checking if port 8080 is available...
netstat -ano | findstr :8080 >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARNING] Port 8080 is in use
) else (
    echo [OK] Port 8080 is available
)

echo.
echo ==========================================
echo Build Recommendations
echo ==========================================
echo.

REM Provide build recommendations
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker not available. Recommended build command:
    echo   mvn clean install -DskipTests
) else (
    echo Docker available. Recommended build command:
    echo   mvn clean install
)

echo.
echo Alternative build commands:
echo   mvn clean install -DskipTests           # Skip all tests
echo   mvn clean install -Djacoco.skip=true    # Skip coverage check
echo   mvn clean package                       # Build without install
echo.

REM Try a quick validation
echo ==========================================
echo Running Maven Validation
echo ==========================================
echo.

mvn validate

if %errorlevel% equ 0 (
    echo [OK] Maven validation successful
    echo.
    echo You can now run: mvn clean install
) else (
    echo [ERROR] Maven validation failed
    echo Check the errors above and fix before building
    exit /b 1
)

