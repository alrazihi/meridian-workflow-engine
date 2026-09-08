package com.meridian.application.port.outbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.valueobjects.DocumentId;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(DocumentId id);
    List<Document> findAll();
    boolean existsByIdempotencyKey(String idempotencyKey);
    Document updateStatus(DocumentId id, DocumentStatus newStatus);
}
