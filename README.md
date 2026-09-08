# meridian-workflow-engine

**Enterprise Document Processing & Workflow Orchestration Platform**

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Angular](https://img.shields.io/badge/Angular-18-red)
![Docker](https://img.shields.io/badge/Docker_Compose-blue)
![Kafka](https://img.shields.io/badge/Kafka-3.7-black)

> **Important:** This is an original reference implementation demonstrating enterprise ECM/workflow concepts. It is **not affiliated with or derived from** proprietary IBM FileNet, Laserfiche, or any customer implementations. No official compatibility with IBM FileNet or Laserfiche is claimed.

## Purpose

Meridian is an event-driven enterprise workflow engine that manages document-centric business processes. It demonstrates senior-level software architecture through a realistic, production-pattern reference implementation.

**Business scenario:** An organization receives invoices, contracts, and compliance documents. Each document must be validated, routed to appropriate reviewers, potentially transformed or enriched, approved/rejected, and archived with full lineage. Some documents trigger downstream payment runs or regulatory reporting.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring for Kafka |
| Frontend | Angular 18, NgRx, Angular Material |
| Database | PostgreSQL 16, Flyway |
| Messaging | Apache Kafka 3.7 |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven, Angular CLI |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Documentation | OpenAPI 3, Mermaid |

## Quick Start

```bash
git clone https://github.com/alrazihi/meridian-workflow-engine.git
cd meridian-workflow-engine
docker compose up --build
```

- Backend API: http://localhost:8080/api/v1
- Frontend: http://localhost:4200
- API Docs: http://localhost:8080/swagger-ui.html

## Documentation

- [Architecture](ARCHITECTURE.md)
- [Threat Model](THREAT-MODEL.md)
- [ADRs](ADR/)
- [API Documentation](docs/api/openapi.yaml)
- [Database Schema](docs/database/schema.md)
- [Security Model](docs/security/security-model.md)
- [Test Strategy](docs/testing/test-strategy.md)
- [Deployment](docs/deployment/docker-compose.md)

## Architecture Highlights

- **Hexagonal Architecture** with strict dependency boundaries
- **Domain-Driven Design** with aggregates, value objects, and domain services
- **Event-Driven Workflow** with Kafka for asynchronous processing
- **CQRS** — planned for workflow query optimization
- **OAuth2 Resource Server** with role-based access control
- **Testcontainers** for integration testing
- **Flyway** for versioned database migrations
- **Prometheus + Actuator** for observability

## Project Structure

```
meridian-workflow-engine/
├── backend/
│   ├── meridian-domain/                 # Pure Java, zero dependencies
│   ├── meridian-application/            # Use cases & ports
│   ├── meridian-infrastructure/         # Adapters (JPA, Kafka, Security)
│   ├── meridian-workflow-service/       # Spring Boot application
│   └── pom.xml
├── frontend/
│   └── angular-admin/                   # Angular 18 SPA
├── docker-compose.yml
├── .github/workflows/ci-cd.yml
└── docs/
```

## License

MIT
