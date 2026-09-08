# ADR-005: Testcontainers for Integration Tests

## Status

Accepted

## Context

Integration tests must run against real PostgreSQL and Kafka instances, not mocks. The tests should be deterministic, fast, and require no external infrastructure setup.

## Decision

Use **Testcontainers** for integration tests.

### Containers
- PostgreSQL 16 for repository tests
- Kafka for messaging tests
- WireMock for HTTP client tests

### Lifecycle
- Containers started/stopped per test class or per test method
- Reused via Docker layer caching in CI
- Flyway migrations run automatically against test container

## Consequences

### Positive
- Tests run against real databases/message brokers
- No external dependencies or mock configurations
- Consistent between local and CI environments
- Strong confidence in production behavior

### Negative
- Slower than pure unit tests (container startup ~2-5s)
- Docker required on all developer machines and CI runners
- Increased CI resource usage

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Embedded PostgreSQL | Not production-equivalent |
| H2 in-memory DB | SQL dialect differences, missing PG features |

## References

- [Testcontainers Documentation](https://www.testcontainers.org/)
