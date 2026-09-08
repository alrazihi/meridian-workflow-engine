package com.meridian.application.service;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.StartWorkflowUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.domain.model.DocumentId;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.service.DocumentValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultWorkflowOrchestrator implements StartWorkflowUseCase, CompleteTaskUseCase {

    private final DocumentRepository documentRepository;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final DocumentValidator documentValidator;

    public DefaultWorkflowOrchestrator(
            DocumentRepository documentRepository,
            EventPublisher eventPublisher,
            NotificationService notificationService,
            DocumentValidator documentValidator) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.documentValidator = documentValidator;
    }

    @Override
    public WorkflowInstance start(String documentId) {
        DocumentId docId = DocumentId.from(documentId);
        var document = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        DocumentValidator.ValidationResult transitionResult = documentValidator
                .validateTransition(document.status(), DocumentStatus.ROUTED);
        if (!transitionResult.valid()) {
            throw new IllegalStateException(transitionResult.errorMessage());
        }

        WorkflowInstance instance = WorkflowInstance.start(docId, documentId);
        WorkflowTask reviewTask = WorkflowTask.create(instance.id(), "group:reviewers", "REVIEW");
        reviewTask.assign();

        List<WorkflowTask> tasks = new ArrayList<>();
        tasks.add(reviewTask);

        DocumentEvent event = DocumentEvent.create(
                docId.value(),
                "WORKFLOW_STARTED",
                "{\"workflowId\":\"" + instance.id().value() + "\"}",
                documentId
        );
        eventPublisher.publish(event);
        notificationService.notifyTaskAssigned("group:reviewers", reviewTask.id(), "REVIEW");

        return instance;
    }

    @Override
    public WorkflowInstance completeTask(String workflowId, String taskId, String decision, String comments) {
        WorkflowInstance instance = getStatus(workflowId);
        WorkflowTask task = instance.tasks().stream()
                .filter(t -> t.id().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        WorkflowTask completedTask = task.complete("current-user", comments);

        if ("APPROVED".equals(decision)) {
            WorkflowInstance updated = instance.withState("COMPLETED");
            DocumentEvent event = DocumentEvent.create(
                    instance.documentId().value(),
                    "WORKFLOW_COMPLETED",
                    "{\"decision\":\"APPROVED\"}",
                    instance.correlationId()
            );
            eventPublisher.publish(event);
            return updated;
        } else {
            WorkflowInstance updated = instance.withState("REJECTED");
            DocumentEvent event = DocumentEvent.create(
                    instance.documentId().value(),
                    "WORKFLOW_REJECTED",
                    "{\"decision\":\"REJECTED\"}",
                    instance.correlationId()
            );
            eventPublisher.publish(event);
            return updated;
        }
    }

    public WorkflowInstance getStatus(String workflowId) {
        return null;
    }
}
