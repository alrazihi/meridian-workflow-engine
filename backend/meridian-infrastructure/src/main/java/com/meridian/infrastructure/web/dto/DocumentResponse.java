package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;

import java.time.Instant;
import java.util.Map;

public record DocumentResponse(
        String documentId,
        DocumentStatus status,
        DocumentType type,
        String priority,
        Map<String, Object> metadata,
        Instant createdAt,
        String contentHash
) {
    public static DocumentResponse from(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.id().value(),
                document.status(),
                document.type(),
                document.priority().name(),
                document.metadata(),
                document.createdAt(),
                document.contentHash()
        );
    }
}
