package com.meridian.application.service;

import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.service.DocumentValidator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public class DefaultDocumentIngestionService implements IngestDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final EventPublisher eventPublisher;
    private final DocumentValidator documentValidator;
    private final NotificationService notificationService;

    public DefaultDocumentIngestionService(
            DocumentRepository documentRepository,
            EventPublisher eventPublisher,
            DocumentValidator documentValidator,
            NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.documentValidator = documentValidator;
        this.notificationService = notificationService;
    }

    @Override
    public Document ingest(byte[] fileContent, DocumentType type, String priority, Map<String, Object> metadata, String idempotencyKey) {
        String contentHash = computeHash(fileContent);
        Map<String, String> stringMetadata = metadata.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));

        if (idempotencyKey != null && documentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalArgumentException("Duplicate idempotency key: " + idempotencyKey);
        }

        Document document = Document.create(contentHash, type, stringMetadata, idempotencyKey);
        DocumentValidator.ValidationResult validationResult = documentValidator.validate(document);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.errorMessage());
        }

        Document saved = documentRepository.save(document);
        DocumentEvent createdEvent = DocumentEvent.create(
                saved.id(),
                "DOCUMENT_CREATED",
                "{}",
                saved.id().value()
        );
        eventPublisher.publish(createdEvent);

        return saved;
    }

    private String computeHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

