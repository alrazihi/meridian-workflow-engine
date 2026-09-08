package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.DocumentId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Document(
        DocumentId id,
        String contentHash,
        Map<String, String> metadata,
        DocumentStatus status,
        DocumentType type,
        Priority priority,
        Instant createdAt,
        Instant updatedAt,
        long version,
        String idempotencyKey
) {
    public Document {
        Objects.requireNonNull(id, "id cannot be null");
    }

    public static Document create(String contentHash, DocumentType type, Map<String, String> metadata, String idempotencyKey) {
        return new Document(
                DocumentId.generate(),
                contentHash,
                metadata,
                DocumentStatus.RECEIVED,
                type,
                Priority.NORMAL,
                Instant.now(),
                Instant.now(),
                0L,
                idempotencyKey
        );
    }
}
