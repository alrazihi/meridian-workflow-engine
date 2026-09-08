-- V4__add_audit_index.sql
-- Event stream optimization for audit queries

CREATE INDEX IF NOT EXISTS idx_document_events_correlation_time
    ON document_events (correlation_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_document_events_type_time
    ON document_events (event_type, occurred_at DESC);
