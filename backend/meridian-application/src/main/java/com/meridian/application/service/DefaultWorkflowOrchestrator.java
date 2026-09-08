package com.meridian.application.service;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.StartWorkflowUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.TaskStatus;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.service.DocumentValidator;

import java.util.List;

public class DefaultWorkflowOrchestrator implements StartWorkflowUseCase, CompleteTaskUseCase {

    private final DocumentRepository documentRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final DocumentValidator documentValidator;

    public DefaultWorkflowOrchestrator(
            DocumentRepository documentRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            TaskRepository taskRepository,
            EventPublisher eventPublisher,
            NotificationService notificationService,
            DocumentValidator documentValidator) {
        this.documentRepository = documentRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.taskRepository = taskRepository;
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
        reviewTask = reviewTask.assign();
        taskRepository.save(reviewTask);

        List<WorkflowTask> tasks = taskRepository.findByWorkflowId(instance.id());
        WorkflowInstance instanceWithTasks = new WorkflowInstance(
                instance.id(),
                instance.documentId(),
                instance.state(),
                instance.context(),
                instance.correlationId(),
                instance.startedAt(),
                instance.completedAt(),
                instance.version(),
                instance.createdAt(),
                tasks
        );

        workflowInstanceRepository.save(instanceWithTasks);

        DocumentEvent event = DocumentEvent.create(
                docId,
                "WORKFLOW_STARTED",
                "{\"workflowId\":\"" + instance.id().value() + "\"}",
                documentId
        );
        eventPublisher.publish(event);
        notificationService.notifyTaskAssigned("group:reviewers", reviewTask.id(), "REVIEW");

        return instanceWithTasks;
    }

    @Override
    public WorkflowInstance completeTask(String workflowId, String taskId, String decision, String comments) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.status() == TaskStatus.COMPLETED) {
            WorkflowInstance current = workflowInstanceRepository.findById(new WorkflowId(workflowId))
                    .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
            return current;
        }

        WorkflowTask completedTask = task.complete("current-user", comments);
        taskRepository.save(completedTask);

        WorkflowInstance instance = workflowInstanceRepository.findById(new WorkflowId(workflowId))
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        List<WorkflowTask> tasks = taskRepository.findByWorkflowId(new WorkflowId(workflowId));
        instance = new WorkflowInstance(
                instance.id(),
                instance.documentId(),
                instance.state(),
                instance.context(),
                instance.correlationId(),
                instance.startedAt(),
                instance.completedAt(),
                instance.version(),
                instance.createdAt(),
                tasks
        );

        if ("APPROVED".equals(decision)) {
            WorkflowInstance updated = instance.withState("COMPLETED");
            workflowInstanceRepository.save(updated);
            DocumentEvent event = DocumentEvent.create(
                    instance.documentId(),
                    "WORKFLOW_COMPLETED",
                    "{\"decision\":\"APPROVED\"}",
                    instance.correlationId()
            );
            eventPublisher.publish(event);
            return updated;
        } else {
            WorkflowInstance updated = instance.withState("REJECTED");
            workflowInstanceRepository.save(updated);
            DocumentEvent event = DocumentEvent.create(
                    instance.documentId(),
                    "WORKFLOW_REJECTED",
                    "{\"decision\":\"REJECTED\"}",
                    instance.correlationId()
            );
            eventPublisher.publish(event);
            return updated;
        }
    }

    @Override
    public WorkflowInstance getStatus(String workflowId) {
        WorkflowId workflowIdVo = WorkflowId.from(workflowId);
        return workflowInstanceRepository.findById(workflowIdVo)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }
}
