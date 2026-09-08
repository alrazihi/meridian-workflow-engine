package com.meridian.infrastructure.persistence.repository;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.valueobjects.WorkflowId;
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
class WorkflowInstanceRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaWorkflowInstanceRepository jpaWorkflowInstanceRepository;

    @Autowired
    private JpaTaskRepository jpaTaskRepository;

    @Test
    void shouldPersistAndRetrieveWorkflowInstance() {
        WorkflowInstance instance = WorkflowInstance.start(new DocumentId("doc-123"), "corr-456");

        WorkflowInstance saved = jpaWorkflowInstanceRepository.save(instance);
        assertThat(saved.id()).isNotNull();

        var found = jpaWorkflowInstanceRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().state()).isEqualTo("STARTED");
        assertThat(found.get().documentId()).isEqualTo(new DocumentId("doc-123"));
    }

    @Test
    void shouldReturnEmptyOptionalForMissingWorkflow() {
        var found = jpaWorkflowInstanceRepository.findById(new WorkflowId("non-existent"));
        assertThat(found).isEmpty();
    }

    @Test
    void shouldLoadTasksWithWorkflowInstance() {
        WorkflowInstance instance = WorkflowInstance.start(new DocumentId("doc-123"), "corr-456");
        WorkflowTask task = WorkflowTask.create(instance.id(), "group:reviewers", "REVIEW");
        jpaTaskRepository.save(task);

        WorkflowInstance saved = jpaWorkflowInstanceRepository.save(instance);
        var found = jpaWorkflowInstanceRepository.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().tasks()).hasSize(1);
        assertThat(found.get().tasks().get(0).action()).isEqualTo("REVIEW");
    }
}
