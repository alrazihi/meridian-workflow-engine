package com.meridian.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.domain.model.DocumentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String documentEventsTopic;
    private final String workflowTasksTopic;
    private final String workflowCompletedTopic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                               @Value("${spring.kafka.topics.document-events}") String documentEventsTopic,
                               @Value("${spring.kafka.topics.workflow-tasks}") String workflowTasksTopic,
                               @Value("${spring.kafka.topics.workflow-completed}") String workflowCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.documentEventsTopic = documentEventsTopic;
        this.workflowTasksTopic = workflowTasksTopic;
        this.workflowCompletedTopic = workflowCompletedTopic;
    }

    @Override
    @Transactional
    public void publish(DocumentEvent event) {
        try {
            String key = event.documentId().value();
            String value = objectMapper.writeValueAsString(event);
            String topic = switch (event.eventType()) {
                case "TASK_ASSIGNED" -> workflowTasksTopic;
                case "WORKFLOW_COMPLETED", "WORKFLOW_REJECTED" -> workflowCompletedTopic;
                default -> documentEventsTopic;
            };
            kafkaTemplate.send(topic, key, value);
            log.info("Published event {} to topic {} for document {}", event.eventType(), topic, event.documentId());
        } catch (Exception e) {
            log.error("Failed to publish event for document {}", event.documentId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
