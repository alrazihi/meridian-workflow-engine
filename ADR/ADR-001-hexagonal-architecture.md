# ADR-001: Hexagonal Architecture with Maven Multi-Module

## Status

Accepted

## Context

We need an architecture that demonstrates senior-level design thinking while remaining practical for a portfolio reference implementation. The system must separate business logic from frameworks, enable testing without infrastructure, and clearly show dependency boundaries.

## Decision

Adopt **Hexagonal Architecture** (Ports & Adapters) implemented as a **Maven multi-module** project with four modules:

1. `meridian-domain` — pure Java, zero framework dependencies
2. `meridian-application` — use cases and ports (Spring-free interfaces)
3. `meridian-infrastructure` — adapters for persistence, messaging, security
4. `meridian-workflow-service` — Spring Boot application wiring

### Dependency Rules

```
meridian-workflow-service
    └── meridian-infrastructure
            └── meridian-application
                    └── meridian-domain
```

No reverse dependencies. Domain cannot import anything from outer layers.

## Consequences

### Positive
- Business logic testable without Spring, database, or Kafka
- Easy to swap implementations (e.g., replace JPA with jOOQ)
- Clear ownership: domain experts own the inner modules
- Framework upgrades isolated to infrastructure layer

### Negative
- More initial boilerplate than layered architecture
- Requires discipline to prevent "leakage" of framework types inward
- Maven multi-module adds build complexity

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Traditional layered (controller/service/repository) | Business logic entangled with framework annotations |
| Clean Architecture with Gradle | Gradle less common in enterprise Java portfolios |
| Single module with package separation | Harder to enforce dependency boundaries |

## References

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring Hexagonal](https://spring.io/blog/2022/02/21/hexagonal-architecture-with-spring-boot)
