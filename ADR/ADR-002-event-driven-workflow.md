# ADR-002: Event-Driven Workflow with Kafka

## Status

Accepted

## Context

Document workflows are inherently long-running and asynchronous. Multiple systems may need to react to document events (audit, notifications, archival). We need a mechanism that provides reliable event delivery, ordering guarantees per document, replayability for recovery, and decoupling between workflow engine and downstream consumers.

## Decision

Use **Apache Kafka** as the event backbone with the following design:

### Topics
- `document.events` — all domain events (created, validated, routed, task_assigned, etc.)
- `workflow.tasks` — task assignment events for notification service
- `workflow.completed` — workflow completion signals

### Topic Configuration
Topics are configurable via `spring.kafka.topics.*` properties in `application.yml`.

### Exactly-Once Semantics
- Producer: idempotent producer with `acks=all`, `enable-idempotency=true`, `transaction-id-prefix`
- Consumer: manual offset commit after processing (`enable-auto-commit=false`)
- Storage: compaction on `document.events` for latest state, retention for `workflow.tasks`

## Consequences

### Positive
- Loose coupling between workflow engine and side effects
- Natural audit trail from event stream
- Enables real-time dashboards and notifications
- Replay capability for debugging and recovery

### Negative
- Operational complexity of Kafka cluster
- eventual consistency between write model and read model
- Schema evolution requires coordination
- Additional testing complexity

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Spring Integration with AMQP | Less streaming-native, harder to scale |
| Database polling | Tight coupling, poor latency, wasteful |
| Synchronous callbacks | Brittle, cascading failures, no replay |

## References

- [Kafka Exactly-Once Semantics](https://kafka.apache.org/documentation/#semantics)
- [Event Sourcing Pattern](https://microservices.io/patterns/data/event-sourcing.html)
