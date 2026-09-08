package com.meridian.infrastructure.persistence.repository;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
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
class TaskRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaTaskRepository jpaTaskRepository;

    @Test
    void shouldPersistAndRetrieveTask() {
        WorkflowTask task = WorkflowTask.create(WorkflowId.generate(), "group:reviewers", "REVIEW");

        WorkflowTask saved = jpaTaskRepository.save(task);
        assertThat(saved.id()).isNotNull();

        var found = jpaTaskRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().action()).isEqualTo("REVIEW");
        assertThat(found.get().status()).isEqualTo("PENDING");
    }

    @Test
    void shouldFindTasksByWorkflowId() {
        WorkflowId workflowId = WorkflowId.generate();
        WorkflowTask task1 = WorkflowTask.create(workflowId, "group:reviewers", "REVIEW");
        WorkflowTask task2 = WorkflowTask.create(workflowId, "group:approvers", "APPROVE");

        jpaTaskRepository.save(task1);
        jpaTaskRepository.save(task2);

        List<WorkflowTask> tasks = jpaTaskRepository.findByWorkflowId(workflowId);

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(WorkflowTask::action).containsExactlyInAnyOrder("REVIEW", "APPROVE");
    }

    @Test
    void shouldReturnEmptyListForUnknownWorkflow() {
        List<WorkflowTask> tasks = jpaTaskRepository.findByWorkflowId(WorkflowId.generate());
        assertThat(tasks).isEmpty();
    }
}
