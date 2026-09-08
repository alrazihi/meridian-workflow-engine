package com.meridian.infrastructure.messaging.kafka;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class EventProcessingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;

    @Test
    void shouldUpdateDocumentStatusOnWorkflowCompletedEvent() throws Exception {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        DocumentEvent event = DocumentEvent.create(
                saved.id(),
                "WORKFLOW_COMPLETED",
                "{\"decision\":\"APPROVED\"}",
                documentId
        );
        kafkaEventPublisher.publish(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(new DocumentId(documentId))
                    .orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.COMPLETED);
        });
    }

    @Test
    void shouldUpdateDocumentStatusOnWorkflowRejectedEvent() throws Exception {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        DocumentEvent event = DocumentEvent.create(
                saved.id(),
                "WORKFLOW_REJECTED",
                "{\"decision\":\"REJECTED\"}",
                documentId
        );
        kafkaEventPublisher.publish(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(new DocumentId(documentId))
                    .orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.REJECTED);
        });
    }

    @Test
    void shouldIgnoreUnknownEventType() throws Exception {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        DocumentEvent event = DocumentEvent.create(
                saved.id(),
                "UNKNOWN_EVENT",
                "{}",
                documentId
        );
        kafkaEventPublisher.publish(event);

        Document unchanged = documentRepository.findById(new DocumentId(documentId))
                .orElseThrow();
        assertThat(unchanged.status()).isEqualTo(DocumentStatus.RECEIVED);
    }
}
