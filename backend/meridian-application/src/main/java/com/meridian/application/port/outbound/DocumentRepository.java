package com.meridian.application.port.outbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;

import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(DocumentId id);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
