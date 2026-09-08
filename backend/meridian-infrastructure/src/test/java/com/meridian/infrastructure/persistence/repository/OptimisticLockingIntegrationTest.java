package com.meridian.infrastructure.persistence.repository;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class OptimisticLockingIntegrationTest {

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
    void shouldDetectConcurrentUpdateConflict() throws Exception {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        Document saved = jpaDocumentRepository.save(document);
        DocumentId documentId = saved.id();

        Document loaded1 = jpaDocumentRepository.findById(documentId).orElseThrow();
        Document loaded2 = jpaDocumentRepository.findById(documentId).orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Future<?> future1 = executor.submit(() -> {
            try {
                Document updated1 = new Document(
                        loaded1.id(), loaded1.contentHash(), loaded1.metadata(),
                        DocumentStatus.VALIDATING, loaded1.type(), loaded1.priority(),
                        loaded1.createdAt(), loaded1.updatedAt(), loaded1.version(),
                        loaded1.idempotencyKey()
                );
                jpaDocumentRepository.save(updated1);
            } finally {
                latch.countDown();
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                Document updated2 = new Document(
                        loaded2.id(), loaded2.contentHash(), loaded2.metadata(),
                        DocumentStatus.ROUTED, loaded2.type(), loaded2.priority(),
                        loaded2.createdAt(), loaded2.updatedAt(), loaded2.version(),
                        loaded2.idempotencyKey()
                );
                jpaDocumentRepository.save(updated2);
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        assertThat(future1.isDone()).isTrue();
        assertThat(future2.isDone()).isTrue();

        assertThatThrownBy(() -> future1.get())
                .hasCauseInstanceOf(OptimisticLockingFailureException.class)
                .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> future2.get())
                .hasCauseInstanceOf(OptimisticLockingFailureException.class)
                .isInstanceOf(Exception.class);

        executor.shutdown();
    }
}
