package com.meridian.application.port.inbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;

public interface QueryDocumentUseCase {
    Document getDocument(DocumentId documentId);
}
