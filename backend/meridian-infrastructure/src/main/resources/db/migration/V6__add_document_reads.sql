-- V6__add_document_reads.sql
-- CQRS read model for optimized document queries

CREATE TABLE IF NOT EXISTS document_reads (
    id VARCHAR(36) PRIMARY KEY,
    content_hash VARCHAR(64) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_document_reads_status
    ON document_reads (status);

CREATE INDEX IF NOT EXISTS idx_document_reads_created
    ON document_reads (created_at DESC);
