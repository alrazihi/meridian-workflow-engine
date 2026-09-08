# Observability and Operations Guide

This document describes what operators can observe, how to troubleshoot common failures, and what metrics/health endpoints are available.

## What Operators Can Observe

### Health Endpoints

| Endpoint | Purpose | Authentication |
|----------|---------|----------------|
| `GET /actuator/health` | Liveness check — is the application running? | None (public) |
| `GET /actuator/health` with auth | Readiness details — DB, Kafka, Redis connectivity | Authenticated |
| `GET /actuator/info` | Application metadata | None (public) |
| `GET /actuator/prometheus` | Metrics in Prometheus format | Authenticated |

**Important:** `/actuator/health` shows details only to authenticated requests. Load balancers can probe the unauthenticated endpoint for liveness without seeing internal state.

### Metrics (Prometheus)

The application exposes Micrometer metrics at `/actuator/prometheus`.

#### Business Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `documents.ingested` | Counter | Total documents successfully ingested |
| `workflows.started` | Counter | Total workflows started |
| `tasks.completed` | Counter | Total tasks completed |
| `events.publish.failures` | Counter | Total event publish failures to Kafka |
| `optimistic.lock.failures` | Counter | Total optimistic lock conflicts |

#### Infrastructure Metrics (from Actuator)

| Metric | Description |
|--------|-------------|
| `jvm.memory.used` | JVM heap memory usage |
| `process.cpu.usage` | CPU usage of the JVM process |
| `hikaricp.connections.active` | Active database connections |
| `hikaricp.connections.pending` | Pending database connection requests |
| `kafka.consumer.lag` | Approximate consumer lag (sum of end offsets) |

### Structured Logging

Logs use a consistent format with MDC correlation IDs:

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO  c.m.a.s.DefaultWorkflowOrchestrator [abc-123] - Workflow started: workflowId=wf-1, documentId=doc-1, correlationId=corr-456, taskId=task-1
```

**Correlation ID propagation:**
- HTTP requests: `X-Correlation-ID` header is generated if missing and returned in the response
- Kafka events: `correlationId` field in the event payload is propagated to MDC during processing
- All log lines within a request/event include the correlation ID in `[correlationId]`

### Audit Log

Audit events are persisted to the `audit_logs` table and queryable via `GET /api/v1/audit` (ADMIN role only).

Current audit coverage:
- Security checks via `@PreAuthorize` (`AuditAspect`)
- Workflow state transitions (`WORKFLOW_STARTED`, `TASK_COMPLETED`)
- Document ingestion (`DOCUMENT_INGESTED`)

## Important Failure Signals

### 1. Database Unavailable

**Symptoms:**
- Health endpoint shows `DOWN` for `db` component
- HTTP 500 errors with `DataAccessResourceFailureException` in logs

**Troubleshooting:**
```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Check database connectivity:
```bash
psql -h localhost -U meridian -d meridian -c "SELECT 1"
```

### 2. Kafka Unavailable

**Symptoms:**
- Health endpoint shows `DOWN` for `kafka` component
- `events.publish.failures` counter increments
- Logs contain: `Failed to publish event for document ...`

**Troubleshooting:**
```bash
# Check Kafka broker health
docker ps | grep kafka

# Check consumer lag
curl -s http://localhost:8080/actuator/prometheus | grep kafka.consumer.lag
```

### 3. Optimistic Lock Failures

**Symptoms:**
- `optimistic.lock.failures` counter increments
- HTTP 409 Conflict responses
- Logs contain: `Optimistic lock failure: ...`

**Cause:** Two concurrent requests modified the same document or task.

**Troubleshooting:**
- Check `optimistic.lock.failures` rate in Prometheus
- If rate is high, investigate concurrent API clients or Kafka consumers processing the same document

### 4. Poison Message / Consumer Stuck

**Symptoms:**
- Kafka consumer lag increasing
- Documents not transitioning to ROUTED/COMPLETED/REJECTED
- Repeated error logs: `Failed to process document event: ...`

**Troubleshooting:**
```bash
# Check consumer lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group meridian-workflow-service --describe

# Find poison message
# Look for document IDs that are stuck in RECEIVED state
curl -s http://localhost:8080/api/v1/documents | jq '.[] | select(.status == "RECEIVED")'
```

**Resolution:** Manually skip the poison message by seeking the consumer group past the bad offset, or fix the malformed event source.

### 5. Partial Workflow Execution

**Symptoms:**
- Workflow instance exists but has no tasks
- Tasks exist but workflow state is still STARTED

**Cause:** This should not happen with `@Transactional` boundaries. If it does, it indicates a transaction boundary bug.

**Troubleshooting:**
```sql
-- Check for orphaned tasks
SELECT t.id, t.workflow_id, w.id as workflow_exists
FROM workflow_tasks t
LEFT JOIN workflow_instances w ON t.workflow_id = w.id
WHERE w.id IS NULL;

-- Check for workflows with missing tasks
SELECT w.id, w.state, COUNT(t.id) as task_count
FROM workflow_instances w
LEFT JOIN workflow_tasks t ON w.id = t.workflow_id
GROUP BY w.id, w.state
HAVING COUNT(t.id) = 0;
```

### 6. Stale Cache

**Symptoms:**
- Document status in cache differs from database
- Happens within 10-minute TTL window

**Troubleshooting:**
- Cache is evicted on every status update by `DocumentEventConsumer`
- If stale, check that Kafka events are being processed (see #4)
- Manual cache eviction: restart the application or wait for TTL expiry

## Metrics to Alert On

| Metric | Alert Condition | Severity |
|--------|-----------------|----------|
| `optimistic.lock.failures` rate | > 10/min for 5 min | Warning |
| `events.publish.failures` rate | > 5/min for 5 min | Warning |
| `kafka.consumer.lag` | > 1000 for 10 min | Warning |
| `hikaricp.connections.pending` | > 5 for 2 min | Critical |
| JVM memory usage | > 85% for 5 min | Warning |

## Troubleshooting Common Failures

### Workflow stuck in STARTED

1. Check if task exists: `GET /api/v1/workflows/{workflowId}`
2. Check if task is assigned: look for `currentTask` in response
3. Check Kafka consumer lag — event may not have been processed
4. Check logs for `Workflow started:` to verify creation

### Document stuck in RECEIVED

1. Check if `WORKFLOW_STARTED` event was published (search logs for `Published event WORKFLOW_STARTED`)
2. Check if `DocumentEventConsumer` processed it (search logs for `Updated document ... status to ROUTED`)
3. Check consumer lag — event may be queued
4. Check for optimistic lock failures in consumer logs

### Duplicate task completion

The system is idempotent — calling `completeTask` twice returns the same COMPLETED state. No action needed.

### High optimistic lock failure rate

1. Identify conflicting operations: concurrent `start()` calls on same document, or concurrent `completeTask()` on same task
2. Check for retry loops in clients
3. Consider adding application-level retry with backoff for `OptimisticLockingFailureException`

## Configuration for Production

### application.yml

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000
      validation-timeout: 5000
      max-lifetime: 1800000
      minimum-idle: 5
      maximum-pool-size: 20
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
      max-poll-interval-ms: 300000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### Logging

Logs are plain text by default. For JSON logging in production, configure a `logback-spring.xml` with `logstash-logback-encoder`.

### Secrets

Never log secrets. The following are never logged:
- `WEBHOOK_SECRET`
- `JWT_SECRET_KEY`
- `DATABASE_PASSWORD`
- Document content or metadata values (only document IDs and types are logged)

## References

- ADR-002: Event-Driven Workflow with Kafka
- ADR-005: Testcontainers for Integration Tests
- ADR-007: Concurrency Control and State Machine Validation
- `docs/RESILIENCE.md`
