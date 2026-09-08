-- V7__add_document_acl.sql
-- Document-level access control for ABAC

CREATE TABLE IF NOT EXISTS document_acl (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_acl UNIQUE (document_id, actor, permission)
);

CREATE INDEX IF NOT EXISTS idx_document_acl_document
    ON document_acl (document_id);

CREATE INDEX IF NOT EXISTS idx_document_acl_actor
    ON document_acl (actor);
