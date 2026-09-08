package com.meridian;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class IdempotencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DefaultDocumentIngestionService ingestionService;

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        ingestionService.ingest(
                "content1".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                "idempotency-key-1"
        );

        assertThatThrownBy(() -> ingestionService.ingest(
                "content2".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-002"),
                "idempotency-key-1"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Duplicate idempotency key");
    }

    @Test
    void shouldAllowSameContentWithDifferentIdempotencyKey() {
        Document doc1 = ingestionService.ingest(
                "content".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                "key-1"
        );

        Document doc2 = ingestionService.ingest(
                "content".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                "key-2"
        );

        assertThat(doc1.id()).isNotEqualTo(doc2.id());
    }

    @Test
    void shouldAllowNullIdempotencyKey() {
        Document doc1 = ingestionService.ingest(
                "content1".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of(),
                null
        );

        Document doc2 = ingestionService.ingest(
                "content2".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of(),
                null
        );

        assertThat(doc1.id()).isNotEqualTo(doc2.id());
    }
}
