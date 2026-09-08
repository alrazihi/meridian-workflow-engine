-- V1__initial_schema.sql
-- Initial schema for meridian-workflow-engine

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY,
    content_hash VARCHAR(64) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    type VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    idempotency_key VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_document_status CHECK (status IN ('RECEIVED','VALIDATING','ROUTED','PROCESSING','COMPLETED','REJECTED','ARCHIVED')),
    CONSTRAINT chk_document_priority CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    CONSTRAINT uq_documents_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_documents_status_priority_created
    ON documents (status, priority, created_at DESC);

CREATE TABLE IF NOT EXISTS workflow_instances (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
    state VARCHAR(30) NOT NULL,
    context JSONB NOT NULL DEFAULT '{}',
    correlation_id UUID NOT NULL DEFAULT gen_random_uuid(),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_instances_document
    ON workflow_instances (document_id);

CREATE TABLE IF NOT EXISTS workflow_tasks (
    id VARCHAR(36) PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    assignee VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    completed_by VARCHAR(100),
    comments TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status CHECK (status IN ('PENDING','ASSIGNED','COMPLETED','SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_workflow_tasks_workflow_status
    ON workflow_tasks (workflow_id, status);

CREATE TABLE IF NOT EXISTS document_events (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    causation_id UUID,
    correlation_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_events_document_time
    ON document_events (document_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS routing_rules (
    id VARCHAR(36) PRIMARY KEY,
    predicate JSONB NOT NULL,
    target_workflow_id VARCHAR(36) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_definitions (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    definition JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(name, version)
);
