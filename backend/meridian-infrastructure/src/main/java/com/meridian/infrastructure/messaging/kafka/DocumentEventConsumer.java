package com.meridian.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.DocumentReadRepository;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.infrastructure.cache.CachingDocumentQueryService;
import com.meridian.infrastructure.web.filter.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DocumentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventConsumer.class);
    private final DocumentRepository documentRepository;
    private final DocumentReadRepository documentReadRepository;
    private final CachingDocumentQueryService cachingDocumentQueryService;
    private final ObjectMapper objectMapper;

    public DocumentEventConsumer(DocumentRepository documentRepository, DocumentReadRepository documentReadRepository, CachingDocumentQueryService cachingDocumentQueryService, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentReadRepository = documentReadRepository;
        this.cachingDocumentQueryService = cachingDocumentQueryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"document.events", "workflow.tasks", "workflow.completed"}, groupId = "meridian-workflow-service")
    public void onDocumentEvent(String message) {
        String correlationId = null;
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            String documentId = (String) event.get("documentId");
            correlationId = (String) event.get("correlationId");

            if (correlationId != null && !correlationId.isBlank()) {
                MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);
            }

            if (documentId == null) {
                log.warn("Received event without documentId: eventType={}", eventType);
                return;
            }

            switch (eventType) {
                case "WORKFLOW_STARTED" -> updateStatus(new DocumentId(documentId), DocumentStatus.ROUTED);
                case "WORKFLOW_COMPLETED" -> updateStatus(new DocumentId(documentId), DocumentStatus.COMPLETED);
                case "WORKFLOW_REJECTED" -> updateStatus(new DocumentId(documentId), DocumentStatus.REJECTED);
                default -> log.debug("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process document event: event={}", message, e);
            throw new RuntimeException("Failed to process document event", e);
        } finally {
            if (correlationId != null) {
                MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            }
        }
    }

    private void updateStatus(DocumentId documentId, DocumentStatus newStatus) {
        Document updated = documentRepository.updateStatus(documentId, newStatus);
        documentReadRepository.updateStatus(documentId, newStatus);
        cachingDocumentQueryService.evictDocument(documentId);
        log.info("Updated document {} status to {} in write and read models; cache evicted", documentId.value(), newStatus);
    }
}