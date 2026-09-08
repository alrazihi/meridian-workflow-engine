package com.meridian;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class FailurePathIntegrationTest {

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

    @Autowired
    private DefaultWorkflowOrchestrator workflowOrchestrator;

    @Test
    void shouldFailIngestWhenValidationFails() {
        assertThatThrownBy(() -> ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of(), // empty metadata should fail validation
                null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Document metadata is required");
    }

    @Test
    void shouldFailWorkflowStartForNonExistentDocument() {
        assertThatThrownBy(() -> workflowOrchestrator.start("non-existent-doc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void shouldFailWorkflowStartForInvalidStatusTransition() {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        assertThat(document.status()).isEqualTo(DocumentStatus.RECEIVED);

        assertThatThrownBy(() -> workflowOrchestrator.start(document.id().value()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void shouldFailCompleteTaskForUnknownTask() {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());

        assertThatThrownBy(() -> workflowOrchestrator.completeTask(
                instance.id().value(),
                "unknown-task",
                "APPROVED",
                "test"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Task not found");
    }

    @Test
    void shouldFailQueryNonExistentWorkflow() {
        assertThatThrownBy(() -> workflowOrchestrator.getStatus("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workflow not found");
    }
}
