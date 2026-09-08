package com.meridian.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.domain.model.DocumentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DocumentEvent event) {
        try {
            String key = event.documentId().value();
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("document.events", key, value);
            log.info("Published event {} for document {}", event.eventType(), event.documentId());
        } catch (Exception e) {
            log.error("Failed to publish event for document {}", event.documentId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
