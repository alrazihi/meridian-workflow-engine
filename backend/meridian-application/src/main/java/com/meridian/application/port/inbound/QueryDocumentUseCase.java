package com.meridian.application.port.inbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;

import java.util.List;

public interface QueryDocumentUseCase {
    Document getDocument(DocumentId documentId);
    List<Document> listDocuments();
}
