# Resilience and Failure Behavior

This document describes how the Meridian workflow engine behaves under failure conditions, and what guarantees the system provides.

## Design Philosophy

The system is designed for **partial failure tolerance** with clear consistency boundaries:
- **Database operations** are atomic within orchestration boundaries
- **Event publishing** is best-effort and asynchronous
- **State transitions** are idempotent and validated
- **No distributed transactions** — eventual consistency between write model and read model is accepted

## Failure Scenarios

### 1. Database Unavailable

**Scenario**: PostgreSQL is down or unreachable during a request.

**Current Behavior**:
- Spring throws `DataAccessResourceFailureException` or `CannotGetJdbcConnectionException`
- The exception propagates to the client as a 500 error
- No partial state is committed because the connection fails before any SQL executes

**Safety**: ✅ Safe — no partial state possible

**Test**: Covered by Spring Boot's default exception handling. No custom test needed.

---

### 2. Transaction Rollback (Partial Workflow Execution)

**Scenario**: An exception occurs mid-orchestration after some repositories have saved but before the operation completes.

**Example**: In `DefaultWorkflowOrchestrator.start()`, the task is saved but the workflow instance fails to save.

**Current Behavior**:
- `@Transactional` on `start()` and `completeTask()` ensures all DB operations within the method are atomic
- If any exception occurs, the entire transaction rolls back
- No orphaned tasks or partial workflow instances

**Safety**: ✅ Fixed — transaction boundaries added

**Test**: `ResilienceIntegrationTest.shouldRollbackWorkflowStartIfInstanceSaveFails` (documents expected behavior)

---

### 3. Downstream Service Unavailable (Kafka Down)

**Scenario**: Kafka broker is unavailable when the application tries to publish an event.

**Current Behavior**:
- `KafkaEventPublisher.publish()` throws an exception
- The exception is caught by `publishEventAsync()` in the orchestration layer
- The DB transaction commits successfully
- The event is not published, but the workflow state is consistent
- The failure is logged for operational alerting

**Safety**: ✅ Safe — DB state is consistent; event loss is acceptable and can be replayed

**Test**: `ResilienceIntegrationTest.shouldContinueProcessingAfterMalformedEvent` (documents that consumer continues after failures)

**Expected Behavior**:
```
Workflow started successfully → DB committed
Event publish failed → logged, not retried automatically
Operator must replay event manually if needed
```

---

### 4. Duplicate Event

**Scenario**: Kafka redelivers the same event (e.g., after consumer restart or redelivery).

**Current Behavior**:
- `DocumentEventConsumer.updateStatus()` checks `currentStatus == newStatus`
- If the document is already in the target status, the update is a no-op
- The event is safely ignored

**Safety**: ✅ Safe — idempotent by design

**Test**: `ResilienceIntegrationTest.shouldHandleDuplicateKafkaEventIdempotently`

---

### 5. Consumer Restart

**Scenario**: The Kafka consumer crashes and restarts (e.g., deployment, OOM kill).

**Current Behavior**:
- `enable-auto-commit=false` — offsets are not committed until processing succeeds
- If the consumer crashes before committing, the message is redelivered
- Duplicate events are handled by the idempotency guard (see #4)

**Safety**: ✅ Safe — at-least-once delivery with idempotent processing

**Test**: Simulated via `DocumentEventConsumerIntegrationTest` with direct `onDocumentEvent()` calls

---

### 6. Message Processing Failure (Transient)

**Scenario**: A transient error occurs during event processing (e.g., optimistic lock conflict).

**Current Behavior**:
- `OptimisticLockingFailureException` propagates out of `updateStatus()`
- Kafka does not commit the offset
- The message is redelivered
- On redelivery, the consumer reads the updated document and sees the new status, so the update becomes a no-op

**Safety**: ✅ Safe — redelivery leads to idempotent no-op

**Test**: `ConcurrencyDeterministicTest.shouldDetectConcurrentDocumentStatusUpdates`

---

### 7. Retry Exhaustion / Poison Message

**Scenario**: A message is permanently unprocessable (e.g., malformed JSON, missing required fields).

**Current Behavior**:
- `ObjectMapper.readValue()` throws `JsonProcessingException` or `IllegalArgumentException`
- The exception is caught in `onDocumentEvent()` and rethrown as `RuntimeException`
- Kafka redelivers the message indefinitely
- **No dead-letter queue (DLQ)** — the message will spin forever

**Safety**: ⚠️ Partial — the consumer continues processing other messages, but the poison message causes infinite redelivery

**Mitigation**:
- Operational: Monitor consumer lag and manually skip poison messages
- Future: Add a DLQ or max-redelivery limit (not implemented — see ADR-007)

**Test**: `ResilienceIntegrationTest.shouldHandlePoisonMessageWithoutInfiniteLoop` (documents that consumer continues after poison message)

---

### 8. Malformed Event

**Scenario**: The event payload is not valid JSON or is missing required fields.

**Current Behavior**:
- `ObjectMapper.readValue()` throws exception
- Exception propagates, Kafka redelivers
- Consumer continues processing subsequent messages

**Safety**: ✅ Safe — consumer is not crashed; other messages continue processing

**Test**: `RetryAndResilienceIntegrationTest.shouldContinueProcessingAfterMalformedEvent`

---

### 9. Stale State (Cache)

**Scenario**: A document's status is updated, but the cache still serves the old value.

**Current Behavior**:
- `CachingDocumentQueryService` caches documents for 10 minutes
- `DocumentEventConsumer.updateStatus()` calls `cachingDocumentQueryService.evictDocument()` after every status change
- Subsequent reads fetch fresh data from the database

**Safety**: ✅ Fixed — cache is evicted on every status update

**Test**: `ResilienceIntegrationTest.shouldNotServeStaleDocumentAfterStatusUpdate`

---

### 10. Partial Workflow Execution

**Scenario**: The application crashes or is redeployed mid-workflow (e.g., after task creation but before workflow instance save).

**Current Behavior**:
- `@Transactional` on orchestration methods ensures atomicity
- If the transaction is interrupted, all DB changes are rolled back
- No partial state on restart

**Safety**: ✅ Fixed — transaction boundaries prevent partial state

**Test**: Documented via `ResilienceIntegrationTest.shouldRollbackWorkflowStartIfInstanceSaveFails`

---

### 11. Network Timeout

**Scenario**: Database or Kafka network is slow or temporarily unavailable.

**Current Behavior**:
- Database: Spring's default connection pool timeout applies (configurable via `spring.datasource.hikari.*`)
- Kafka: Spring Kafka's default producer/consumer timeouts apply (configurable via `spring.kafka.*`)

**Safety**: ⚠️ Relies on default timeouts — no custom retry or circuit breaker

**Recommendation**: Configure timeouts explicitly in `application.yml` for production. No code changes needed.

---

### 12. Worker Crash

**Scenario**: A thread processing a request crashes (e.g., `OutOfMemoryError`, `StackOverflowError`).

**Current Behavior**:
- If within a `@Transactional` method, the transaction is marked for rollback
- Spring's transaction interceptor catches `Error` subclasses and rolls back
- Partial DB state is rolled back

**Safety**: ✅ Safe — `@Transactional` catches `Error` and rolls back

---

### 13. Application Restart

**Scenario**: The application restarts (e.g., deployment, config change).

**Current Behavior**:
- In-flight requests are terminated
- `@Transactional` ensures DB changes are rolled back for interrupted transactions
- Kafka consumer offsets are not committed for unprocessed messages
- Messages are redelivered after restart

**Safety**: ✅ Safe — no partial state; redelivery ensures processing continues

---

## Configuration for Production

The following properties should be explicitly set in production `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000
      validation-timeout: 5000
      max-lifetime: 1800000
  kafka:
    producer:
      retries: 3
      acks: all
      delivery-timeout-ms: 120000
      request-timeout-ms: 30000
    consumer:
      max-poll-records: 10
      session-timeout-ms: 10000
      heartbeat-interval-ms: 3000
```

## Monitoring Recommendations

| Metric | Why |
|--------|-----|
| Kafka consumer lag | Detects poison messages or processing bottlenecks |
| Database connection pool usage | Detects connection exhaustion |
| `OptimisticLockingFailureException` rate | Detects high contention on documents/tasks |
| Event publish failures | Detects Kafka availability issues |
| Transaction rollback rate | Detects partial failures in orchestration |

## References

- ADR-002: Event-Driven Workflow with Kafka
- ADR-007: Concurrency Control and State Machine Validation
