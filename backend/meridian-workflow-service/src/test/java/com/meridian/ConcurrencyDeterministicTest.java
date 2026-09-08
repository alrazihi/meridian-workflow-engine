package com.meridian;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.TaskStatus;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.infrastructure.messaging.kafka.DocumentEventConsumer;
import com.meridian.infrastructure.persistence.repository.JpaDocumentRepository;
import com.meridian.infrastructure.persistence.repository.JpaTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
class ConcurrencyDeterministicTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private DefaultDocumentIngestionService ingestionService;

    @Autowired
    private DefaultWorkflowOrchestrator workflowOrchestrator;

    @Autowired
    private JpaTaskRepository jpaTaskRepository;

    @Autowired
    private JpaDocumentRepository jpaDocumentRepository;

    @Autowired
    private DocumentEventConsumer documentEventConsumer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // ==================== WORKFLOW TASK CONCURRENCY ====================

    @Test
    void shouldAllowExactlyOneConcurrentTaskCompletion() throws Exception {
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

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Future<?> future1 = executor.submit(() -> {
            try {
                workflowOrchestrator.completeTask(
                        instance.id().value(),
                        task.id(),
                        "APPROVED",
                        "Completion 1"
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                workflowOrchestrator.completeTask(
                        instance.id().value(),
                        task.id(),
                        "APPROVED",
                        "Completion 2"
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        WorkflowTask finalTask = jpaTaskRepository.findById(task.id()).orElseThrow();
        assertThat(finalTask.status()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void shouldRejectDoubleCompletionIdempotently() {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());
        WorkflowTask task = instance.tasks().get(0);

        WorkflowInstance firstCompletion = workflowOrchestrator.completeTask(
                instance.id().value(),
                task.id(),
                "APPROVED",
                "First completion"
        );
        assertThat(firstCompletion.state()).isEqualTo(WorkflowState.COMPLETED);

        WorkflowInstance secondCompletion = workflowOrchestrator.completeTask(
                instance.id().value(),
                task.id(),
                "APPROVED",
                "Second completion attempt"
        );
        assertThat(secondCompletion.state()).isEqualTo(WorkflowState.COMPLETED);
    }

    // ==================== DOCUMENT STATUS CONCURRENCY ====================

    @Test
    void shouldDetectConcurrentDocumentStatusUpdates() throws Exception {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        DocumentId docId = document.id();
        String event1 = String.format(
                "{\"eventType\":\"WORKFLOW_STARTED\",\"documentId\":\"%s\",\"workflowId\":\"wf-1\"}",
                docId.value()
        );
        String event2 = String.format(
                "{\"eventType\":\"WORKFLOW_STARTED\",\"documentId\":\"%s\",\"workflowId\":\"wf-2\"}",
                docId.value()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Future<?> future1 = executor.submit(() -> {
            try {
                documentEventConsumer.onDocumentEvent(event1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                documentEventConsumer.onDocumentEvent(event2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = jpaDocumentRepository.findById(docId).orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.ROUTED);
        });
    }

    @Test
    void shouldProcessSequentialStatusTransitionsDeterministically() {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        DocumentId docId = document.id();

        documentEventConsumer.onDocumentEvent(String.format(
                "{\"eventType\":\"WORKFLOW_STARTED\",\"documentId\":\"%s\",\"workflowId\":\"wf-1\"}",
                docId.value()
        ));

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = jpaDocumentRepository.findById(docId).orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.ROUTED);
        });

        documentEventConsumer.onDocumentEvent(String.format(
                "{\"eventType\":\"WORKFLOW_COMPLETED\",\"documentId\":\"%s\",\"workflowId\":\"wf-1\"}",
                docId.value()
        ));

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = jpaDocumentRepository.findById(docId).orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.COMPLETED);
        });
    }

    // ==================== WORKFLOW INSTANCE CONCURRENCY ====================

    @Test
    void shouldDetectConcurrentWorkflowStartOnSameDocument() throws Exception {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        // First start workflow to transition document to ROUTED
        workflowOrchestrator.start(document.id().value());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    workflowOrchestrator.start(document.id().value());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(0);
        assertThat(failureCount.get()).isEqualTo(2);
    }

    // ==================== IDEMPOTENCY ====================

    @Test
    void shouldHandleDuplicateKafkaEventIdempotently() {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        DocumentId docId = document.id();
        String event = String.format(
                "{\"eventType\":\"WORKFLOW_COMPLETED\",\"documentId\":\"%s\",\"workflowId\":\"wf-1\"}",
                docId.value()
        );

        documentEventConsumer.onDocumentEvent(event);
        documentEventConsumer.onDocumentEvent(event);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = jpaDocumentRepository.findById(docId).orElseThrow();
            assertThat(updated.status()).isEqualTo(DocumentStatus.COMPLETED);
        });
    }

    // ==================== OPTIMISTIC LOCKING ====================

    @Test
    void shouldDetectOptimisticLockFailureOnTaskUpdate() throws Exception {
        Document document = ingestionService.ingest(
                "test".getBytes(),
                DocumentType.INVOICE,
                "NORMAL",
                Map.of("vendorId", "VEND-001"),
                null
        );

        WorkflowInstance instance = workflowOrchestrator.start(document.id().value());
        WorkflowTask task = instance.tasks().get(0);

        WorkflowTask loaded1 = jpaTaskRepository.findById(task.id()).orElseThrow();
        WorkflowTask loaded2 = jpaTaskRepository.findById(task.id()).orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Future<?> future1 = executor.submit(() -> {
            try {
                WorkflowTask completed1 = loaded1.complete("user1", "Comment 1");
                jpaTaskRepository.save(completed1);
            } finally {
                latch.countDown();
            }
        });

        Future<?> future2 = executor.submit(() -> {
            try {
                WorkflowTask completed2 = loaded2.complete("user2", "Comment 2");
                jpaTaskRepository.save(completed2);
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        assertThat(future1.isDone()).isTrue();
        assertThat(future2.isDone()).isTrue();

        assertThatThrownBy(() -> future1.get())
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);

        assertThatThrownBy(() -> future2.get())
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);

        executor.shutdown();
    }
}