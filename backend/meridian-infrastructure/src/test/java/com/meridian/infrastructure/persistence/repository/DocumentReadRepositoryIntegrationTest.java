package com.meridian.infrastructure.persistence.repository;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DocumentReadRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaDocumentReadRepository jpaDocumentReadRepository;

    @Test
    void shouldSaveAndFindByStatus() {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null,
                "test-tenant"
        );

        jpaDocumentReadRepository.save(document);
        List<Document> received = jpaDocumentReadRepository.findByStatus(DocumentStatus.RECEIVED);

        assertThat(received).hasSizeGreaterThanOrEqualTo(1);
        assertThat(received.get(0).contentHash()).isEqualTo("hash123");
    }

    @Test
    void shouldFindByType() {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null,
                "test-tenant"
        );

        assertThat(invoices).hasSizeGreaterThanOrEqualTo(1);
        assertThat(invoices.get(0).type()).isEqualTo(DocumentType.INVOICE);
    }
}
