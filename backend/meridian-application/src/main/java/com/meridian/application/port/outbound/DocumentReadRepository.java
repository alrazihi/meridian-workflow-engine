package com.meridian.application.port.outbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;

import java.util.List;

public interface DocumentReadRepository {
    void save(Document document);
    List<Document> findByStatus(DocumentStatus status);
    List<Document> findByType(DocumentType type);
    Document updateStatus(com.meridian.domain.model.valueobjects.DocumentId id, DocumentStatus newStatus);
}
