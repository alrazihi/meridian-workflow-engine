package com.meridian.application.service;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;

import java.util.List;

public class DefaultDocumentQueryService implements QueryDocumentUseCase {

    private final DocumentRepository documentRepository;

    public DefaultDocumentQueryService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Document getDocument(DocumentId documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    @Override
    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }
}
