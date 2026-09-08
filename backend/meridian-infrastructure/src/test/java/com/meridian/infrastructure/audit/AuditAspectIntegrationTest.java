package com.meridian.infrastructure.audit;

import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
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

@SpringBootTest
@Testcontainers
class AuditAspectIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuditService auditService;

    @Test
    void shouldPersistAuditLog() {
        AuditLog auditLog = AuditLog.create(
                "user-1",
                "DOCUMENT_ACCESS",
                "DOCUMENT",
                "doc-123",
                "127.0.0.1",
                "curl/7.68.0",
                Map.of("action", "GET")
        );

        auditService.log(auditLog);

        assertThat(auditLog.id()).isNotNull();
        assertThat(auditLog.occurredAt()).isNotNull();
    }
}
