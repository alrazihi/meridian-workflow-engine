package com.meridian.application.service;

import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.AuditLog;
import com.meridian.domain.service.DocumentValidator;
import com.meridian.infrastructure.observability.WorkflowMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public class DefaultDocumentIngestionService implements IngestDocumentUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultDocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final EventPublisher eventPublisher;
    private final DocumentValidator documentValidator;
    private final NotificationService notificationService;
    private final WorkflowMetrics workflowMetrics;
    private final AuditService auditService;

    public DefaultDocumentIngestionService(
            DocumentRepository documentRepository,
            EventPublisher eventPublisher,
            DocumentValidator documentValidator,
            NotificationService notificationService,
            WorkflowMetrics workflowMetrics,
            AuditService auditService) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.documentValidator = documentValidator;
        this.notificationService = notificationService;
        this.workflowMetrics = workflowMetrics;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public Document ingest(byte[] fileContent, DocumentType type, String priority, Map<String, Object> metadata, String idempotencyKey) {
        String contentHash = computeHash(fileContent);
        Map<String, String> stringMetadata = metadata.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null ? e.getValue().toString() : null
                ));

        if (idempotencyKey != null && documentRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Duplicate idempotency key rejected: idempotencyKey={}", idempotencyKey);
            throw new IllegalArgumentException("Duplicate idempotency key: " + idempotencyKey);
        }

        Document document = Document.create(contentHash, type, stringMetadata, idempotencyKey, "test-tenant");
        DocumentValidator.ValidationResult validationResult = documentValidator.validate(document);

        if (!validationResult.isValid()) {
            log.warn("Document validation failed: {}", validationResult.errorMessage());
            throw new IllegalArgumentException(validationResult.errorMessage());
        }

        Document saved = documentRepository.save(document);
        log.info("Document ingested: documentId={}, type={}, status={}, idempotencyKey={}",
                saved.id().value(), type, DocumentStatus.RECEIVED, idempotencyKey);
        workflowMetrics.incrementDocumentIngested();

        auditService.log(AuditLog.create(
                "system",
                "DOCUMENT_INGESTED",
                "DOCUMENT",
                saved.id().value(),
                Map.of("type", type.name(), "idempotencyKey", idempotencyKey)
        ));

        publishEventAsync(() -> {
            DocumentEvent createdEvent = DocumentEvent.create(
                    saved.id(),
                    "DOCUMENT_CREATED",
                    "{}",
                    saved.id().value()
            );
            eventPublisher.publish(createdEvent);
        });

        return saved;
    }

    private void publishEventAsync(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Failed to publish async side-effect for document ingestion", e);
        }
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

