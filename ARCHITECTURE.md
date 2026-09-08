# Architecture Documentation

## C4 Model

### Level 1: System Context

```
┌─────────────┐     HTTPS      ┌──────────────────────────┐
│   Browser   │◄──────────────►│  meridian-workflow-api   │
│  (Angular)  │                │  (Spring Boot + Kafka)    │
└─────────────┘                └──────────────────────────┘
                                           │
                        ┌──────────────────┼──────────────────┐
                        │                  │                  │
                   ┌────▼────┐        ┌────▼────┐        ┌────▼────┐
                   │PostgreSQL│        │  Kafka  │        │  Redis  │
                   │  (JDBC) │        │(events) │        │ (cache) │
                   └─────────┘        └─────────┘        └─────────┘
```

### Level 2: Container

```
┌─────────────────────────────────────────────────────────────┐
│                    meridian-workflow-service                 │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ REST API    │  │  Webhook    │  │  Scheduled Tasks    │  │
│  │ Controllers │  │  Ingress    │  │  (cleanup, etc.)    │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │             │
│  ┌──────▼────────────────▼─────────────────────▼──────────┐  │
│  │              Application Layer (Use Cases)             │  │
│  └──────┬────────────────────┬───────────────────┬────────┘  │
│         │                    │                   │           │
│  ┌──────▼──────┐    ┌────────▼────────┐  ┌──────▼────────┐  │
│  │  Domain     │    │  Persistence     │  │  Messaging    │  │
│  │  Services   │    │  Adapters        │  │  Adapters     │  │
│  │  Entities   │    │  (JPA/Flyway)    │  │  (Kafka)      │  │
│  └─────────────┘    └─────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Level 3: Component

#### Domain Layer (`meridian-domain`)
- Pure Java, zero framework dependencies
- Aggregates: `Document`, `WorkflowInstance`
- Value Objects: `DocumentId`, `WorkflowId`, `Money`, `DocumentReference`
- Domain Services: `DocumentValidator`, `WorkflowRouter`, `CompensationManager`
- Exceptions: `DomainException`, `WorkflowTransitionException`

#### Application Layer (`meridian-application`)
- **Inbound Ports** (use cases):
  - `IngestDocumentUseCase`
  - `StartWorkflowUseCase`
  - `CompleteTaskUseCase`
  - `QueryWorkflowStatusUseCase`
- **Outbound Ports** (interfaces):
  - `DocumentRepository`
  - `EventPublisher`
  - `NotificationService`
- **Services**: orchestration without framework coupling

#### Infrastructure Layer (`meridian-infrastructure`)
- **Persistence**: JPA entities, Flyway migrations, Spring Data repositories
- **Messaging**: Kafka producer/consumer, Avro schema management
- **Security**: JWT authentication converter, method security config
- **Web**: REST controllers, DTOs, exception handlers, OpenAPI config

#### Presentation Layer (`meridian-workflow-service`)
- Spring Boot application entry point
- Configuration classes (Jackson, security, Kafka, Actuator)
- Cross-cutting concerns: logging, metrics, tracing

## Design Principles

1. **Dependency Rule**: Dependencies point inward only. Domain has no outward dependencies.
2. **Interface Segregation**: Small, focused ports rather than fat repositories.
3. **Explicit Dependencies**: All collaborators passed via constructor injection.
4. **Fail Fast**: Validate at domain boundaries, reject invalid state transitions immediately.
5. **Immutable Events**: All domain events are append-only and immutable.
6. **Idempotency**: All mutation endpoints accept idempotency keys.
7. **Security by Design**: Authorization evaluated at every layer.

## Key Architectural Decisions

See [ADR directory](ADR/) for detailed rationale.

| ADR | Decision |
|-----|----------|
| ADR-001 | Hexagonal Architecture with Maven multi-module |
| ADR-002 | Event-Driven Workflow with Kafka |
| ADR-003 | JWT-Based OAuth2 Resource Server |
| ADR-004 | Flyway for Database Versioning |
| ADR-005 | Testcontainers for Integration Tests |
| ADR-006 | CQRS for Workflow Queries |
