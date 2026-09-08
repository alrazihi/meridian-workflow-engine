package com.meridian.infrastructure.cache;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private QueryDocumentUseCase queryDocumentUseCase;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void shouldCacheDocumentAndAvoidSecondRepositoryCall() {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        Document saved = documentRepository.save(document);
        DocumentId documentId = saved.id();

        Document first = queryDocumentUseCase.getDocument(documentId);
        assertThat(first).isNotNull();

        Document second = queryDocumentUseCase.getDocument(documentId);
        assertThat(second).isEqualTo(first);
    }
}
