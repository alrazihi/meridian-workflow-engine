-- V3__add_retention_policy.sql
-- Document retention columns for compliance

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS retention_policy VARCHAR(50),
    ADD COLUMN IF NOT EXISTS retention_expires_at TIMESTAMPTZ;
