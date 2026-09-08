package com.meridian;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.domain.model.WorkflowTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ConcurrencyIntegrationTest {

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
    void shouldHandleConcurrentIngestsWithDifferentIdempotencyKeys() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        int successCount = 0;

        for (int i = 0; i < 10; i++) {
            final String key = "concurrent-key-" + i;
            executor.submit(() -> {
                try {
                    ingestionService.ingest(
                            ("content-" + key).getBytes(),
                            DocumentType.INVOICE,
                            "NORMAL",
                            Map.of("index", key),
                            key
                    );
                    successCount++;
                } catch (Exception e) {
                    // count failures if any
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount).isEqualTo(10);
    }

    @Test
    void shouldHandleConcurrentWorkflowCompletions() throws Exception {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());
        List<WorkflowTask> tasks = instance.tasks();
        WorkflowTask task = tasks.get(0);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    workflowOrchestrator.completeTask(
                            instance.id().value(),
                            task.id(),
                            "APPROVED",
                            "Concurrent completion"
                    );
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Other exceptions count as failures
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(4);

        WorkflowInstance finalState = workflowOrchestrator.getStatus(instance.id().value());
        assertThat(finalState).isNotNull();
        assertThat(finalState.state()).isEqualTo(WorkflowState.COMPLETED);
    }
}
