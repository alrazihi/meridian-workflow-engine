package com.meridian.application.service;

import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.StartWorkflowUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.service.DocumentValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
public class DefaultDocumentIngestionService implements IngestDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final EventPublisher eventPublisher;
    private final DocumentValidator documentValidator;
    private final StartWorkflowUseCase startWorkflowUseCase;
    private final NotificationService notificationService;

    public DefaultDocumentIngestionService(
            DocumentRepository documentRepository,
            EventPublisher eventPublisher,
            DocumentValidator documentValidator,
            StartWorkflowUseCase startWorkflowUseCase,
            NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.documentValidator = documentValidator;
        this.startWorkflowUseCase = startWorkflowUseCase;
        this.notificationService = notificationService;
    }

    @Override
    public Document ingest(MultipartFile file, DocumentType type, String priority, Map<String, Object> metadata, String idempotencyKey) {
        try {
            String contentHash = computeHash(file.getBytes());
            Map<String, String> stringMetadata = metadata.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue() != null ? e.getValue().toString() : null
                    ));
            Document document = Document.create(contentHash, type, stringMetadata);
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
        } catch (IOException e) {
            throw new IllegalStateException("Failed to process document", e);
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

