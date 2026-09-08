# ADR-006: CQRS for Workflow Queries

## Status

Accepted (implementation in progress)

## Context

Workflow status queries are read-heavy and require joining multiple tables. The write model is optimized for transactional consistency, while the read model can tolerate eventual consistency and benefit from denormalization.

## Decision

Apply **CQRS** selectively to workflow query operations.

### Write Model
- JPA entities with optimistic locking
- Transactional boundaries at use case level
- Events published after successful commit

### Read Model
- Materialized view or separate query-optimized tables
- Updated via Kafka consumer listening to domain events
- Served through dedicated query ports

### Scope
- NOT applied to document CRUD (simple enough for single model)
- Applied to: workflow status, task lists, audit trails, dashboards

## Consequences

### Positive
- Read queries don't compete with writes for database resources
- Read model can be scaled independently
- Enables real-time dashboards via Kafka consumers
- Clear separation of concerns

### Negative
- eventual consistency between write and read models
- Additional code to maintain two models
- Complexity for simple use cases

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Single model with optimized queries | Locks read and write scalability together |
| Full CQRS with separate databases | Overkill for portfolio scope |
| Cache-aside with Redis | Good for hot data, but not full query replacement |

## References

- [CQRS Pattern](https://microservices.io/patterns/data/cqrs.html)
