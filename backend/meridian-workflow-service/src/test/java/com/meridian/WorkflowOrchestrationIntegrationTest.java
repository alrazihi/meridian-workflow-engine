package com.meridian;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
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

@SpringBootTest
@Testcontainers
class WorkflowOrchestrationIntegrationTest {

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
    void shouldIngestAndStartWorkflow() {
        Document document = ingestionService.ingest(
                "test content".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        assertThat(document.id()).isNotNull();
        assertThat(document.status()).isEqualTo(DocumentStatus.RECEIVED);

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());
        assertThat(instance.id()).isNotNull();
        assertThat(instance.state()).isEqualTo(WorkflowState.STARTED);
        assertThat(instance.tasks()).hasSize(1);
    }

    @Test
    void shouldCompleteWorkflowTask() {
        Document document = ingestionService.ingest(
                "test content".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());
        List<WorkflowTask> tasks = instance.tasks();
        assertThat(tasks).hasSize(1);

        WorkflowTask task = tasks.get(0);
        WorkflowInstance completed = workflowOrchestrator.completeTask(
                instance.id().value(),
                task.id(),
                "APPROVED",
                "Looks good"
        );

        assertThat(completed.state()).isEqualTo(WorkflowState.COMPLETED);
    }
}
