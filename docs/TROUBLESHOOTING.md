# Troubleshooting Guide - Distributed Job Scheduler

**Last Updated**: 2026-03-07

---

## Common Build Issues

### 1. Missing Flyway Migration Files

**Error:**
```
Error creating bean with name 'flyway'
Caused by: org.flywaydb.core.api.FlywayException:
  Unable to find migrations location in: [classpath:db/migration]
```

**Cause:**
- Flyway is enabled in `application.yml`
- Migration files don't exist at `src/main/resources/db/migration/`

**Solution:**
✅ **FIXED** - Created Flyway SQL migration files (V1-V4)

**Verify:**
```bash
# Check file exists
ls -la src/main/resources/db/changelog/db.changelog-master.xml

# Should see the file
```

---

### 2. Java Version Mismatch

**Error:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.12.1:compile
[ERROR] Source option 21 is no longer supported. Use 21 or later.
```

**Cause:**
- JAVA_HOME points to Java 17 or earlier
- Project requires Java 21

**Solution:**
```bash
# Check Java version
java -version

# Should show: openjdk version "21" or higher

# If not, set JAVA_HOME (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

# If not, set JAVA_HOME (Linux/Mac)
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# Verify
java -version
mvn -version
```

---

### 3. Maven Dependency Resolution Failure

**Error:**
```
[ERROR] Failed to execute goal on project distributed-job-scheduler: 
Could not resolve dependencies
```

**Cause:**
- Network issues
- Corrupted Maven cache
- Repository unavailable

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository  # Linux/Mac
rmdir /s /q %USERPROFILE%\.m2\repository  # Windows

# Force update dependencies
mvn clean install -U

# If behind proxy, configure Maven settings
# Edit ~/.m2/settings.xml
```

---

### 4. Database Connection Failure (During Tests)

**Error:**
```
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
Caused by: java.sql.SQLException: Communications link failure
```

**Cause:**
- MySQL not running
- Testcontainers can't start Docker
- Docker not installed

**Solution:**

**Option 1: Skip tests during build**
```bash
mvn clean install -DskipTests
```

**Option 2: Start MySQL manually**
```bash
docker-compose up -d mysql
mvn clean install
```

**Option 3: Use H2 for tests (in-memory)**
- Tests are configured to use Testcontainers
- Requires Docker to be running

---

### 5. Port Already in Use

**Error:**
```
Web server failed to start. Port 8080 was already in use.
```

**Cause:**
- Another application using port 8080
- Previous instance still running

**Solution:**
```bash
# Find process using port 8080 (Windows)
netstat -ano | findstr :8080
taskkill /PID <process_id> /F

# Find process using port 8080 (Linux/Mac)
lsof -ti:8080
kill -9 $(lsof -ti:8080)

# Or change port in application.yml
server:
  port: 8081
```

---

### 6. Redisson Connection Failure

**Error:**
```
Unable to connect to Redis server: localhost:6379
```

**Cause:**
- Redis not running
- Wrong Redis host/port

**Solution:**

**Option 1: Start Redis**
```bash
docker-compose up -d redis
```

**Option 2: Disable Redisson temporarily**

Edit `application-dev.yml`:
```yaml
# Comment out Redis configuration
# spring:
#   data:
#     redis:
#       host: localhost
#       port: 6379
```

**Note**: This will cause leader election features to fail. Only use for initial setup.

---

### 7. Lombok Not Working

**Error:**
```
[ERROR] cannot find symbol
  symbol:   method builder()
  location: class Job
```

**Cause:**
- Lombok not installed in IDE
- Annotation processing not enabled

**Solution:**

**IntelliJ IDEA:**
1. Install Lombok plugin: `Settings → Plugins → Search "Lombok" → Install`
2. Enable annotation processing: `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`
3. Restart IDE

**Eclipse:**
1. Download lombok.jar from https://projectlombok.org/download
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation
4. Restart Eclipse

**VS Code:**
1. Install "Lombok Annotations Support" extension
2. Restart VS Code

---

### 8. JaCoCo Coverage Check Failure

**Error:**
```
[ERROR] Rule violated for package com.scheduler: 
lines covered ratio is 0.45, but expected minimum is 0.60
```

**Cause:**
- Code coverage below 60%
- Not enough tests written

**Solution:**

**Option 1: Skip coverage check**
```bash
mvn clean install -Djacoco.skip=true
```

**Option 2: Lower coverage threshold temporarily**

Edit `pom.xml`:
```xml
<minimum>0.30</minimum>  <!-- Lowered from 0.60 -->
```

**Option 3: Write more tests** (recommended for later)

---

### 9. Preview Features Error

**Error:**
```
[ERROR] preview features are not enabled
```

**Cause:**
- `--enable-preview` flag in compiler args
- Not all Java 21 features are stable

**Solution:**

**Option 1: Remove preview features flag**

Edit `pom.xml`:
```xml
<configuration>
    <source>${java.version}</source>
    <target>${java.version}</target>
    <!-- Remove this:
    <compilerArgs>
        <arg>--enable-preview</arg>
    </compilerArgs>
    -->
</configuration>
```

**Option 2: Add preview flag to Surefire**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

---

## Build Verification Checklist

Before running `mvn clean install`, verify:

- [ ] Java 21 installed: `java -version`
- [ ] Maven 3.9+ installed: `mvn -version`
- [ ] JAVA_HOME set correctly
- [ ] Flyway migrations exist: `src/main/resources/db/migration/V*.sql`
- [ ] Docker running (for Testcontainers): `docker ps`
- [ ] No process on port 8080: `netstat -ano | findstr :8080` (Windows) or `lsof -ti:8080` (Linux/Mac)

---

## Successful Build Output

When everything works, you should see:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.234 s
[INFO] Finished at: 2026-03-07T10:30:00Z
[INFO] ------------------------------------------------------------------------
```

**Generated artifacts:**
- `target/distributed-job-scheduler-1.0.0-SNAPSHOT.jar`
- `target/classes/` - Compiled classes
- `target/test-classes/` - Compiled test classes
- `target/jacoco.exec` - Coverage data
- `target/site/jacoco/` - Coverage report

---

## Quick Fixes for Common Scenarios

### Scenario 1: First Time Build

```bash
# Minimal build (skip tests, skip coverage)
mvn clean install -DskipTests -Djacoco.skip=true

# Should succeed if:
# - Java 21 installed
# - Flyway migrations exist
# - No compilation errors
```

### Scenario 2: Build with Tests (Requires Docker)

```bash
# Start infrastructure
docker-compose up -d mysql redis

# Build with tests
mvn clean install

# Should succeed if:
# - Docker running
# - MySQL and Redis containers healthy
```

### Scenario 3: Build Without Infrastructure

```bash
# Skip tests that require database/Redis
mvn clean install -DskipTests

# Or use H2 in-memory database for tests
# (Already configured in application-test.yml)
```

---

## Getting Help

If you're still stuck:

1. **Check logs**: Look for the first error in Maven output
2. **Enable debug**: Run with `-X` flag: `mvn clean install -X`
3. **Check dependencies**: `mvn dependency:tree`
4. **Validate POM**: `mvn validate`
5. **Check effective POM**: `mvn help:effective-pom`

---

**Last Updated**: 2026-03-07  
**Next Review**: After Phase 1 completion

