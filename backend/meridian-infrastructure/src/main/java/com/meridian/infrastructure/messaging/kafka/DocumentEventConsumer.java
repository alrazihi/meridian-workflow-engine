package com.meridian.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.DocumentReadRepository;
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
    private final DocumentReadRepository documentReadRepository;
    private final ObjectMapper objectMapper;

    public DocumentEventConsumer(DocumentRepository documentRepository, DocumentReadRepository documentReadRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentReadRepository = documentReadRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"document.events", "workflow.tasks", "workflow.completed"}, groupId = "meridian-workflow-service")
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
                case "WORKFLOW_STARTED" -> updateStatus(new DocumentId(documentId), DocumentStatus.ROUTED);
                case "WORKFLOW_COMPLETED" -> updateStatus(new DocumentId(documentId), DocumentStatus.COMPLETED);
                case "WORKFLOW_REJECTED" -> updateStatus(new DocumentId(documentId), DocumentStatus.REJECTED);
                default -> log.debug("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process document event: {}", message, e);
            throw new RuntimeException("Failed to process document event", e);
        }
    }

    private void updateStatus(DocumentId documentId, DocumentStatus newStatus) {
        Document updated = documentRepository.updateStatus(documentId, newStatus);
        documentReadRepository.updateStatus(documentId, newStatus);
        log.info("Updated document {} status to {} in write and read models", documentId.value(), newStatus);
    }
}