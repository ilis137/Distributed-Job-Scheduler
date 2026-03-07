# Distributed Job Scheduler

A highly available, fault-tolerant distributed job scheduling system built with Java 21 and Spring Boot, demonstrating advanced distributed systems concepts for technical interviews.

## 🎯 Project Goals

This project showcases:
- **Leader Election**: Redis-based leader election with automatic failover
- **Distributed Locking**: Redlock algorithm to prevent duplicate job execution
- **Fault Tolerance**: Automatic recovery from node failures
- **Observability**: Comprehensive metrics, logging, and health checks
- **Production-Ready**: Docker, Kubernetes, CI/CD ready

## 🏗️ Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for comprehensive architecture documentation including:
- High-level system design
- Component interactions
- Leader election process
- Job execution flow
- Database schema
- Deployment architecture

## 🚀 Quick Start

### Prerequisites

- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.9+
- Docker & Docker Compose

### Local Development

1. **Start infrastructure services**
   ```bash
   cd deployment/docker
   docker-compose up -d mysql redis
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API: http://localhost:8080/api
   - Health: http://localhost:8080/actuator/health
   - Metrics: http://localhost:8080/actuator/prometheus

### Multi-Node Cluster

Run a 3-node cluster with Docker Compose:

```bash
cd deployment/docker
docker-compose up -d
```

This starts:
- 3 scheduler nodes (ports 8080, 8081, 8082)
- MySQL database
- Redis cluster

**Note**: Prometheus and Grafana are disabled by default. They will be enabled in Phase 4 (Observability). See [docs/OBSERVABILITY_STRATEGY.md](./docs/OBSERVABILITY_STRATEGY.md)

## 📚 Documentation

- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - Development progress tracker and phase index
- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Comprehensive architecture documentation
- **[DIAGRAMS_ASCII.md](./DIAGRAMS_ASCII.md)** - ASCII architecture diagrams
- **[docs/features/](./docs/features/)** - Feature-specific documentation

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest=*Test

# Run only integration tests
mvn test -Dtest=*IntegrationTest

# Run with coverage
mvn test jacoco:report
```

## 🛠️ Technology Stack

**Backend:**
- Java 21 (Virtual Threads, Records, Pattern Matching)
- Spring Boot 3.2.3
- Spring Data JPA (Hibernate 6.4)
- Redisson 3.27.0 (Redis client)
- Flyway 10.8.1 (Database migrations)

**Infrastructure:**
- Redis 7.2+ (Coordination)
- MySQL 8.0+ (Persistence)
- Prometheus + Grafana (Observability)
- Docker & Kubernetes (Deployment)

## 📊 Key Features

### Phase 1: Core Infrastructure ✅
- [x] Project structure and Maven setup
- [x] Database schema with Flyway (V1-V4 migrations)
- [x] Core domain entities (Job, JobExecution, SchedulerNode)
- [x] JPA repositories
- [ ] Coordination layer (leader election, distributed locking)
- [ ] REST API controllers
- [ ] Job executor with virtual threads

### Phase 2: Leader Election & Failover (Coming Soon)
- [ ] Redis-based leader election
- [ ] Heartbeat mechanism
- [ ] Automatic failover
- [ ] Fencing tokens

### Phase 3: Distributed Locking (Coming Soon)
- [ ] Redlock implementation
- [ ] Idempotency service
- [ ] Retry logic with exponential backoff
- [ ] Job state machine

### Phase 4: Observability (Deferred)
- [ ] Prometheus metrics
- [ ] Custom health indicators
- [ ] Structured logging
- [ ] Distributed tracing

**Note**: Observability features deferred to focus on core distributed systems functionality first.

## 🤝 Contributing

This is a portfolio project for technical interviews. Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Scheduler Team** - *Initial work*

## 🙏 Acknowledgments

- Inspired by production job schedulers like Quartz, Airflow, and Temporal
- Built to demonstrate distributed systems expertise for technical interviews
- Designed with production-ready patterns and best practices

---

**Status**: 🚧 Phase 1 in progress  
**Last Updated**: 2026-03-07

