package com.meridian;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class DocumentEventConsumerIntegrationTest {

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
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldUpdateDocumentStatusOnWorkflowCompletedEvent() {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null,
                "test-tenant"
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        String eventJson = String.format(
                "{\"eventType\":\"WORKFLOW_COMPLETED\",\"documentId\":\"%s\",\"workflowId\":\"wf-123\"}",
                documentId
        );
        kafkaTemplate.send("document.events", documentId, eventJson);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(new DocumentId(documentId))
                    .orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.COMPLETED);
        });
    }

    @Test
    void shouldUpdateDocumentStatusOnWorkflowRejectedEvent() {
        Document document = Document.create(
                "hash456",
                DocumentType.RECEIPT,
                Map.of("store", "Store-1"),
                null,
                "test-tenant"
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        String eventJson = String.format(
                "{\"eventType\":\"WORKFLOW_REJECTED\",\"documentId\":\"%s\",\"workflowId\":\"wf-456\"}",
                documentId
        );
        kafkaTemplate.send("document.events", documentId, eventJson);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(new DocumentId(documentId))
                    .orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.REJECTED);
        });
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        Document document = Document.create(
                "hash789",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-002"),
                null,
                "test-tenant"
        );
        Document saved = documentRepository.save(document);
        String documentId = saved.id().value();

        String eventJson = String.format(
                "{\"eventType\":\"UNKNOWN_EVENT\",\"documentId\":\"%s\",\"workflowId\":\"wf-789\"}",
                documentId
        );
        kafkaTemplate.send("document.events", documentId, eventJson);

        Document unchanged = documentRepository.findById(new DocumentId(documentId))
                .orElseThrow();
        assertThat(unchanged.status()).isEqualTo(DocumentStatus.RECEIVED);
    }

    @Test
    void shouldNotUpdateStatusWhenDocumentNotFound() {
        String nonExistentId = "non-existent-doc-id";

        String eventJson = String.format(
                "{\"eventType\":\"WORKFLOW_COMPLETED\",\"documentId\":\"%s\",\"workflowId\":\"wf-999\"}",
                nonExistentId
        );
        kafkaTemplate.send("document.events", nonExistentId, eventJson);

        await().pollDelay(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var found = documentRepository.findById(new DocumentId(nonExistentId));
            assertThat(found).isEmpty();
        });
    }
}
