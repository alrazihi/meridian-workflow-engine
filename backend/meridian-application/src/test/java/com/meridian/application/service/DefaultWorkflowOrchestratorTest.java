package com.meridian.application.service;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.application.port.outbound.WorkflowInstanceRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.valueobjects.WorkflowId;
import com.meridian.domain.service.DocumentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWorkflowOrchestratorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DefaultWorkflowOrchestrator orchestrator;

    @Test
    void shouldStartWorkflowForValidDocument() {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        document = new Document(
                document.id(), document.contentHash(), document.metadata(),
                DocumentStatus.VALIDATING, document.type(), document.priority(),
                document.createdAt(), document.updatedAt(), document.version(), document.idempotencyKey()
        );
        when(documentRepository.findById(new DocumentId("doc-123"))).thenReturn(Optional.of(document));

        WorkflowInstance savedInstance = WorkflowInstance.start(new DocumentId("doc-123"), "corr-456");
        when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(savedInstance);

        WorkflowTask task = WorkflowTask.create(savedInstance.id(), "group:reviewers", "REVIEW");
        when(taskRepository.save(any(WorkflowTask.class))).thenReturn(task);
        when(taskRepository.findByWorkflowId(savedInstance.id())).thenReturn(List.of(task));

        WorkflowInstance result = orchestrator.start("doc-123");

        assertThat(result).isNotNull();
        assertThat(result.state()).isEqualTo(WorkflowState.STARTED);
        assertThat(result.tasks()).hasSize(1);
        verify(eventPublisher).publish(any(DocumentEvent.class));
        verify(notificationService).notifyTaskAssigned(any(), any(), any());
    }

    @Test
    void shouldThrowWhenDocumentNotFound() {
        when(documentRepository.findById(new DocumentId("missing"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.start("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void shouldThrowWhenDocumentStatusTransitionInvalid() {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.findById(new DocumentId("doc-123"))).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> orchestrator.start("doc-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void shouldCompleteTaskAndTransitionToApproved() {
        WorkflowInstance instance = WorkflowInstance.start(new DocumentId("doc-123"), "corr-456");
        WorkflowTask task = WorkflowTask.create(instance.id(), "group:reviewers", "REVIEW");
        when(workflowInstanceRepository.findById(new WorkflowId(instance.id().value()))).thenReturn(Optional.of(instance));
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(WorkflowTask.class))).thenReturn(task);
        when(taskRepository.findByWorkflowId(new WorkflowId(instance.id().value()))).thenReturn(List.of(task));

        WorkflowInstance completed = orchestrator.completeTask(instance.id().value(), task.id(), "APPROVED", "Looks good");

        assertThat(completed.state()).isEqualTo(WorkflowState.COMPLETED);
        verify(eventPublisher).publish(any(DocumentEvent.class));
    }

    @Test
    void shouldCompleteTaskAndTransitionToRejected() {
        WorkflowInstance instance = WorkflowInstance.start(new DocumentId("doc-123"), "corr-456");
        WorkflowTask task = WorkflowTask.create(instance.id(), "group:reviewers", "REVIEW");
        when(workflowInstanceRepository.findById(new WorkflowId(instance.id().value()))).thenReturn(Optional.of(instance));
        when(taskRepository.findById(task.id())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(WorkflowTask.class))).thenReturn(task);
        when(taskRepository.findByWorkflowId(new WorkflowId(instance.id().value()))).thenReturn(List.of(task));

        WorkflowInstance rejected = orchestrator.completeTask(instance.id().value(), task.id(), "REJECTED", "Bad");

        assertThat(rejected.state()).isEqualTo(WorkflowState.REJECTED);
    }

    @Test
    void shouldThrowWhenCompletingUnknownTask() {
        when(taskRepository.findById("unknown-task")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.completeTask("wf-1", "unknown-task", "APPROVED", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }
}
