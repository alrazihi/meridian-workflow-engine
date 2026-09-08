package com.meridian.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DocumentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventConsumer.class);
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public DocumentEventConsumer(DocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "document.events", groupId = "meridian-workflow-service")
    public void onDocumentEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            String documentId = (String) event.get("documentId");

            if (documentId == null) {
                log.warn("Received event without documentId: {}", eventType);
                return;
            }

            switch (eventType) {
                case "WORKFLOW_STARTED" -> updateStatus(documentId, DocumentStatus.ROUTED);
                case "WORKFLOW_COMPLETED" -> updateStatus(documentId, DocumentStatus.COMPLETED);
                case "WORKFLOW_REJECTED" -> updateStatus(documentId, DocumentStatus.REJECTED);
                default -> log.debug("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process document event: {}", message, e);
        }
    }

    private void updateStatus(String documentId, DocumentStatus newStatus) {
        documentRepository.findById(new DocumentId(documentId))
                .ifPresent(document -> {
                    DocumentStatus currentStatus = document.status();
                    if (currentStatus != newStatus) {
                        Document updated = new Document(
                                document.id(),
                                document.contentHash(),
                                document.metadata(),
                                newStatus,
                                document.type(),
                                document.priority(),
                                document.createdAt(),
                                document.updatedAt(),
                                document.version() + 1,
                                document.idempotencyKey()
                        );
                        documentRepository.save(updated);
                        log.info("Updated document {} status from {} to {}", documentId, currentStatus, newStatus);
                    }
                });
    }
}
