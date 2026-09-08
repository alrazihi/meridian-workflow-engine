package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.DocumentId;

import java.time.Instant;
import java.util.Objects;

public record DocumentEvent(
    String id,
    DocumentId documentId,
    String eventType,
    String payload,
    Instant occurredAt,
    String causationId,
    String correlationId
) {
    public DocumentEvent {
        Objects.requireNonNull(documentId, "documentId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(correlationId, "correlationId cannot be null");
    }

    public static DocumentEvent create(DocumentId documentId, String eventType, String payload, String correlationId) {
        return new DocumentEvent(
            java.util.UUID.randomUUID().toString(),
            documentId,
            eventType,
            payload,
            Instant.now(),
            null,
            correlationId
        );
    }

    public DocumentEvent withCausation(String causationId) {
        return new DocumentEvent(
            this.id,
            this.documentId,
            this.eventType,
            this.payload,
            this.occurredAt,
            causationId,
            this.correlationId
        );
    }
}
