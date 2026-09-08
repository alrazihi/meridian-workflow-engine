package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document response payload")
public record DocumentResponse(
        @Schema(description = "Unique document identifier", example = "doc-123")
        String documentId,
        @Schema(description = "Current processing status")
        DocumentStatus status,
        @Schema(description = "Type of document")
        DocumentType type,
        @Schema(description = "Processing priority", example = "NORMAL")
        String priority,
        @Schema(description = "User-provided metadata key-value pairs")
        Map<String, Object> metadata,
        @Schema(description = "When the document was created", example = "2024-01-15T10:30:00Z")
        Instant createdAt
) {
    public static DocumentResponse from(Document document) {
        if (document == null) {
            return null;
        }
        Map<String, Object> metadataMap = new HashMap<>(document.metadata());
        return new DocumentResponse(
                document.id().value(),
                document.status(),
                document.type(),
                document.priority().name(),
                metadataMap,
                document.createdAt()
        );
    }
}
