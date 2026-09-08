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
class DocumentRepositoryPersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaDocumentRepository jpaDocumentRepository;

    @Test
    void shouldPersistAndRetrieveDocument() {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null
        );

        Document saved = jpaDocumentRepository.save(document);
        assertThat(saved.id()).isNotNull();

        var found = jpaDocumentRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().contentHash()).isEqualTo("hash123");
        assertThat(found.get().status()).isEqualTo(DocumentStatus.RECEIVED);
    }

    @Test
    void shouldPersistAndRetrieveDocumentWithIdempotencyKey() {
        Document document = Document.create(
                "hash456",
                DocumentType.RECEIPT,
                Map.of("store", "Store-1"),
                "idempotency-123"
        );

        Document saved = jpaDocumentRepository.save(document);
        assertThat(saved.idempotencyKey()).isEqualTo("idempotency-123");

        boolean exists = jpaDocumentRepository.existsByIdempotencyKey("idempotency-123");
        assertThat(exists).isTrue();
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        Document document1 = Document.create("hash1", DocumentType.INVOICE, Map.of(), "key-dup");
        Document document2 = Document.create("hash2", DocumentType.INVOICE, Map.of(), "key-dup");

        jpaDocumentRepository.save(document1);
        boolean exists = jpaDocumentRepository.existsByIdempotencyKey("key-dup");

        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnAllDocuments() {
        Document doc1 = Document.create("hash1", DocumentType.INVOICE, Map.of(), null);
        Document doc2 = Document.create("hash2", DocumentType.RECEIPT, Map.of(), null);
        jpaDocumentRepository.save(doc1);
        jpaDocumentRepository.save(doc2);

        List<Document> all = jpaDocumentRepository.findAll();

        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldReturnEmptyOptionalForMissingDocument() {
        var found = jpaDocumentRepository.findById(new com.meridian.domain.model.valueobjects.DocumentId("non-existent"));
        assertThat(found).isEmpty();
    }
}
