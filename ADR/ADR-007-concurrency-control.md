# ADR-007: Concurrency Control and State Machine Validation

## Status

Accepted

## Context

The workflow engine processes documents through asynchronous state transitions driven by Kafka events and synchronous API calls. Multiple workers, retried API requests, and concurrent Kafka consumers can operate on the same document or task simultaneously. Without proper concurrency controls, the system is vulnerable to:

- Lost updates when two consumers process events for the same document
- Double approval/rejection of the same task
- Duplicate workflow instances on the same document
- Invalid state transitions bypassing business rules

## Decision

Implement a multi-layered concurrency control strategy:

### 1. Optimistic Locking with `@Version`

Add JPA `@Version` to **all** mutable entities: `DocumentEntity`, `WorkflowInstanceEntity`, `WorkflowTaskEntity`, and `DocumentReadEntity`.

- Detects concurrent modifications at the database level
- Throws `OptimisticLockingFailureException` on conflict
- Chosen over pessimistic locking because conflicts are rare (document workflows have low write contention) and retry is cheap

### 2. Domain State Machine Validation

Replace raw String state fields with typed enums and validate transitions at the domain level:

- `DocumentStatus` — enum with validated transitions in `DocumentValidator`
- `TaskStatus` — new enum (`PENDING` → `ASSIGNED` → `COMPLETED`) with guards in `WorkflowTask.complete()` and `WorkflowTask.assign()`

Prevents invalid transitions from ever reaching the persistence layer.

### 3. Idempotency Guards

- **Document ingestion**: unique database constraint on `idempotencyKey` + pre-check via `existsByIdempotencyKey()`
- **Task completion**: domain guard `if (status == COMPLETED) return currentState` — makes `completeTask()` idempotent
- **Kafka consumer**: `updateStatus()` uses repository method that checks current status before writing, making duplicate events no-ops

### 4. Kafka Consumer Error Handling

Remove the blanket `catch (Exception e)` that silently swallowed failures. Instead:

- Let `OptimisticLockingFailureException` propagate so Kafka can redeliver the message
- Non-retryable errors still surface in logs for operational alerting

### 5. Deterministic Concurrency Tests

Add integration tests that assert exact success/failure counts under concurrent load, not just "at least one succeeded":

- `ConcurrencyDeterministicTest.shouldAllowExactlyOneConcurrentTaskCompletion`
- `ConcurrencyDeterministicTest.shouldDetectConcurrentDocumentStatusUpdates`
- `OptimisticLockingIntegrationTest.shouldDetectConcurrentTaskUpdateConflict`
- `ConcurrencyDeterministicTest.shouldRejectDoubleCompletionIdempotently`

## Consequences

### Positive
- Lost updates are detected and surfaced, not silently swallowed
- Double approvals are impossible at the domain level
- Duplicate Kafka events are harmless no-ops
- Retried API calls are safe due to idempotency guards
- Test suite catches regressions in concurrency behavior

### Negative
- Slightly higher latency due to version checks (negligible)
- Optimistic lock failures require retry logic at the API/client layer (not yet implemented)
- Kafka redelivery of failed events requires monitoring to prevent poison-message loops

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Pessimistic locking (`PESSIMISTIC_WRITE`) | Higher contention, potential deadlocks, unnecessary for low-conflict workflows |
| Distributed lock (Redis/Redisson) | Adds infrastructure dependency; optimistic locking is sufficient |
| Event sourcing with append-only log | Major architectural change; current CRUD + events is simpler and sufficient |
| Kafka DLQ for all failures | Adds operational complexity; redelivery with backoff is adequate for transient conflicts |

## References

- [JPA Optimistic Locking](https://docs.spring.io/spring-data/jpa/reference/optimistic-locking.html)
- [Kafka Exactly-Once Semantics](https://kafka.apache.org/documentation/#semantics)
- ADR-002: Event-Driven Workflow with Kafka
- ADR-005: Testcontainers for Integration Tests
