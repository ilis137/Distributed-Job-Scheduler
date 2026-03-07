# Feature: Project Setup & Build Configuration

**Status**: 🚧 IN PROGRESS
**Phase**: Phase 1 - Core Infrastructure
**Started**: 2026-03-07
**Completed**: _Not yet completed_

---

## Purpose

Establish the foundational project structure with Maven build configuration, dependency management, and Spring Boot setup. This provides the base upon which all other features will be built.

**Goals:**
- Create a well-organized Maven multi-module project structure (if needed) or single module
- Configure all necessary dependencies for Spring Boot, JPA, Redis, testing, etc.
- Set up Spring Boot application with proper configuration profiles
- Establish coding standards and build conventions

---

## Implementation Details

### Maven Project Structure

**Project Type**: Single Maven module (can be split into multi-module later if needed)

**Key Dependencies:**
- Spring Boot 3.2.3 (Parent POM)
- Spring Data JPA (Hibernate 6.4)
- Spring Web (REST API)
- Spring Boot Actuator (Health checks, metrics)
- Redisson 3.27.0 (Redis client with Redlock support)
- MySQL Connector
- Flyway 10.8.1 (Database migrations)
- Micrometer (Metrics)
- Lombok (Reduce boilerplate)
- JUnit 5 + Mockito (Testing)
- Testcontainers (Integration testing)

### Directory Structure

```
distributed-job-scheduler/
├── src/main/java/com/scheduler/
│   ├── SchedulerApplication.java          # Main Spring Boot application
│   ├── config/                             # Configuration classes
│   ├── domain/                             # JPA entities
│   ├── repository/                         # Data access layer
│   ├── service/                            # Business logic
│   ├── controller/                         # REST controllers
│   └── ...                                 # Other packages
├── src/main/resources/
│   ├── application.yml                     # Main configuration
│   ├── application-dev.yml                 # Development profile
│   ├── application-prod.yml                # Production profile
│   ├── logback-spring.xml                  # Logging configuration
│   └── db/migration/                       # Flyway migrations
├── src/test/java/com/scheduler/
│   ├── integration/                        # Integration tests
│   ├── unit/                               # Unit tests
│   └── chaos/                              # Chaos engineering tests
└── pom.xml                                 # Maven build file
```

### Spring Boot Application Class

```java
package com.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the Distributed Job Scheduler.
 *
 * This application provides a highly available, fault-tolerant job scheduling
 * system with leader election, distributed locking, and automatic failover.
 *
 * @author Scheduler Team
 * @version 1.0.0
 * @since 2026-03-07
 */
@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
```

---

## Configuration

### Maven POM (pom.xml)

**Key Sections:**
1. **Parent**: Spring Boot Starter Parent 3.2.3
2. **Properties**: Java 21, UTF-8 encoding, dependency versions
3. **Dependencies**: All required libraries
4. **Build Plugins**: Maven compiler, Spring Boot plugin, Surefire (tests)

**Important Properties:**
```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.3</spring-boot.version>
    <redisson.version>3.27.0</redisson.version>
    <flyway.version>10.8.1</flyway.version>
    <testcontainers.version>1.19.6</testcontainers.version>
</properties>
```

### Application Configuration (application.yml)

**Main Configuration:**
```yaml
spring:
  application:
    name: distributed-job-scheduler

  profiles:
    active: dev

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Development Profile (application-dev.yml):**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scheduler_dev
    username: scheduler_user
    password: scheduler_pass


### Verifying the Setup

```bash
# Check Java version
java -version  # Should show Java 21

# Check Maven version
mvn -version   # Should show Maven 3.9+

# Verify application starts
mvn spring-boot:run

# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Testing Approach

### Unit Tests
- Test Spring Boot application context loads successfully
- Verify configuration properties are loaded correctly
- Test that all required beans are created

### Integration Tests
- Use Testcontainers to spin up MySQL and Redis
- Verify database connectivity
- Verify Redis connectivity
- Test that Flyway migrations run successfully

### Test Configuration

**application-test.yml:**
```yaml
spring:
  datasource:
    url: jdbc:tc:mysql:8.0:///scheduler_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    baseline-on-migrate: true

redis:
  host: localhost
  port: 6379  # Testcontainers will override this
```

**Example Test:**
```java
@SpringBootTest
@Testcontainers
class SchedulerApplicationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2")
        .withExposedPorts(6379);

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
    }

    @Test
    void databaseConnectionWorks() {
        // Verify database connectivity
    }

    @Test
    void redisConnectionWorks() {
        // Verify Redis connectivity
    }
}
```

---

## Known Limitations

### Current Limitations
1. **Single Module**: Currently a single Maven module. May need to split into multiple modules for better separation of concerns in the future.
2. **No Multi-Region Support**: Configuration assumes single-region deployment.
3. **Basic Security**: No authentication/authorization configured yet (Phase 5).
4. **No Rate Limiting**: API endpoints are not rate-limited yet (Phase 5).

### Future Improvements
1. **Multi-Module Structure**: Split into `scheduler-core`, `scheduler-api`, `scheduler-coordination`, etc.
2. **Configuration Server**: Use Spring Cloud Config for centralized configuration management.
3. **Service Discovery**: Add Eureka or Consul for service discovery in multi-node deployments.
4. **API Gateway**: Add Spring Cloud Gateway for routing and load balancing.
5. **Distributed Tracing**: Add Zipkin or Jaeger for distributed tracing (Phase 4).

---

## Dependencies Reference

### Core Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter Web | 3.2.3 | REST API framework |
| Spring Boot Starter Data JPA | 3.2.3 | Database access with Hibernate |
| Spring Boot Starter Actuator | 3.2.3 | Health checks and metrics |
| Spring Boot Starter Validation | 3.2.3 | Bean validation |
| MySQL Connector | 8.3.0 | MySQL JDBC driver |
| Redisson Spring Boot Starter | 3.27.0 | Redis client with advanced features |
| Flyway Core | 10.8.1 | Database migration tool |
| Flyway MySQL | 10.8.1 | MySQL-specific Flyway support |
| Micrometer Registry Prometheus | 1.12.3 | Prometheus metrics export |
| Lombok | 1.18.30 | Reduce boilerplate code |

### Testing Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter Test | 3.2.3 | Testing framework (JUnit 5, Mockito) |
| Testcontainers MySQL | 1.19.6 | MySQL container for integration tests |
| Testcontainers JUnit Jupiter | 1.19.6 | Testcontainers JUnit 5 integration |
| H2 Database | 2.2.224 | In-memory database for unit tests |

### Build Plugins

| Plugin | Version | Purpose |
|--------|---------|---------|
| Maven Compiler Plugin | 3.12.1 | Compile Java 21 code |
| Spring Boot Maven Plugin | 3.2.3 | Package as executable JAR |
| Maven Surefire Plugin | 3.2.5 | Run unit tests |
| Maven Failsafe Plugin | 3.2.5 | Run integration tests |
| JaCoCo Maven Plugin | 0.8.11 | Code coverage reports |

---

## File Locations

### Configuration Files
- **Main POM**: `pom.xml`
- **Application Config**: `src/main/resources/application.yml`
- **Dev Profile**: `src/main/resources/application-dev.yml`
- **Prod Profile**: `src/main/resources/application-prod.yml`
- **Test Config**: `src/test/resources/application-test.yml`
- **Logging Config**: `src/main/resources/logback-spring.xml`

### Source Files
- **Main Application**: `src/main/java/com/scheduler/SchedulerApplication.java`
- **Test Application**: `src/test/java/com/scheduler/SchedulerApplicationTest.java`

---

## Troubleshooting

### Common Issues

**Issue 1: Java version mismatch**
```
Error: Java version 17 found, but 21 required
```
**Solution**: Install Java 21 and set JAVA_HOME
```bash
export JAVA_HOME=/path/to/java-21
export PATH=$JAVA_HOME/bin:$PATH
```

**Issue 2: Maven dependency resolution fails**
```
Error: Could not resolve dependencies
```
**Solution**: Clear Maven cache and rebuild
```bash
rm -rf ~/.m2/repository
mvn clean install -U
```

**Issue 3: Port 8080 already in use**
```
Error: Port 8080 is already in use
```
**Solution**: Change port in application.yml or kill the process
```bash
# Change port
server.port=8081

# Or kill process
lsof -ti:8080 | xargs kill -9
```

**Issue 4: Database connection fails**
```
Error: Communications link failure
```
**Solution**: Ensure MySQL is running
```bash
docker-compose up -d mysql
```

---

## Next Steps

After completing project setup:

1. ✅ **Verify Build**: Ensure `mvn clean install` succeeds
2. ✅ **Verify Application Starts**: Run `mvn spring-boot:run` and check logs
3. ✅ **Verify Health Endpoint**: Access http://localhost:8080/actuator/health
4. ⏭️ **Proceed to Database Schema**: See [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)
5. ⏭️ **Implement Domain Entities**: See [DOMAIN_MODEL.md](./DOMAIN_MODEL.md)

---

## Related Documentation

- [DEVELOPMENT.md](../../DEVELOPMENT.md) - Main development tracker
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - System architecture
- [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md) - Database design (next step)

---

**Last Updated**: 2026-03-07
**Author**: Development Team
**Reviewers**: _Pending review_

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

redis:
  host: localhost
  port: 6379

logging:
  level:
    com.scheduler: DEBUG
    org.springframework: INFO
```

**Production Profile (application-prod.yml):**
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    baseline-on-migrate: false

redis:
  host: ${REDIS_HOST}
  port: ${REDIS_PORT}
  password: ${REDIS_PASSWORD}

logging:
  level:
    com.scheduler: INFO
    org.springframework: WARN
```

---

## Usage Examples

### Building the Project

```bash
# Clean and build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests only
mvn test

# Package as JAR
mvn clean package
```

### Running the Application

```bash
# Run with default profile (dev)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run packaged JAR
java -jar target/distributed-job-scheduler-1.0.0.jar

# Run with JVM options
java -Xmx2g -Xms1g -jar target/distributed-job-scheduler-1.0.0.jar
```


