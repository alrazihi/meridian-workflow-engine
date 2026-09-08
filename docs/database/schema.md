# Database Schema

## ERD

```
┌─────────────────┐       ┌─────────────────────┐
│   documents     │       │  workflow_instances │
├─────────────────┤       ├─────────────────────┤
│ id (PK)         │──┐    │ id (PK)             │
│ content_hash    │  │    │ document_id (FK)    │
│ metadata (JSONB)│  │    │ state               │
│ status          │  │    │ context (JSONB)     │
│ type            │  │    │ correlation_id      │
│ priority        │  │    │ started_at          │
│ created_at      │  │    │ completed_at        │
│ updated_at      │  │    │ version             │
│ version         │  │    │ created_at          │
└─────────────────┘  │    └─────────────────────┘
                     │
                     │    ┌─────────────────────┐
                     └───►│   workflow_tasks    │
                          ├─────────────────────┤
                          │ id (PK)             │
                          │ workflow_id (FK)    │
                          │ assignee            │
                          │ action              │
                          │ status              │
                          │ due_at              │
                          │ completed_at        │
                          │ completed_by        │
                          │ comments            │
                          │ created_at          │
                          └─────────────────────┘

┌─────────────────┐       ┌─────────────────────┐
│document_events  │       │  routing_rules      │
├─────────────────┤       ├─────────────────────┤
│ id (PK)         │       │ id (PK)             │
│ document_id     │       │ predicate           │
│ event_type      │       │ target_workflow_id  │
│ payload (JSONB) │       │ priority            │
│ occurred_at     │       │ active              │
│ causation_id    │       │ created_at          │
│ correlation_id  │       └─────────────────────┘
└─────────────────┘

┌─────────────────────────────────────┐
│   workflow_definitions             │
├─────────────────────────────────────┤
│ id (PK)                             │
│ name                                │
│ version                             │
│ definition (JSONB)                  │
│ active                              │
│ created_at                          │
└─────────────────────────────────────┘
```

## Migrations

All DDL managed by Flyway in `backend/meridian-infrastructure/src/main/resources/db/migration/`.

| Version | Description |
|---------|-------------|
| V1__initial_schema.sql | Core tables, indexes, constraints |
| V2__add_workflow_definitions.sql | Workflow definition storage |
| V3__add_retention_policy.sql | Document retention columns |
| V4__add_audit_index.sql | Event stream optimization |
