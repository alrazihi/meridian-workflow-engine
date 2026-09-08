package com.meridian.application.service;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.StartWorkflowUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.TaskStatus;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.AuditLog;
import com.meridian.domain.service.DocumentValidator;
import com.meridian.infrastructure.observability.WorkflowMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class DefaultWorkflowOrchestrator implements StartWorkflowUseCase, CompleteTaskUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowOrchestrator.class);

    private final DocumentRepository documentRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final DocumentValidator documentValidator;
    private final WorkflowMetrics workflowMetrics;
    private final AuditService auditService;

    public DefaultWorkflowOrchestrator(
            DocumentRepository documentRepository,
            WorkflowInstanceRepository workflowInstanceRepository,
            TaskRepository taskRepository,
            EventPublisher eventPublisher,
            NotificationService notificationService,
            DocumentValidator documentValidator,
            WorkflowMetrics workflowMetrics,
            AuditService auditService) {
        this.documentRepository = documentRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.documentValidator = documentValidator;
        this.workflowMetrics = workflowMetrics;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public WorkflowInstance start(String documentId) {
        DocumentId docId = DocumentId.from(documentId);
        var document = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

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

        log.info("Workflow started: workflowId={}, documentId={}, correlationId={}, taskId={}",
                instance.id().value(), docId.value(), instance.correlationId(), reviewTask.id());
        workflowMetrics.incrementWorkflowStarted();

        auditService.log(AuditLog.create(
                "system",
                "WORKFLOW_STARTED",
                "WORKFLOW_INSTANCE",
                instance.id().value(),
                Map.of("documentId", docId.value(), "correlationId", instance.correlationId())
        ));

        publishEventAsync(() -> {
            DocumentEvent event = DocumentEvent.create(
                    docId,
                    "WORKFLOW_STARTED",
                    "{\"workflowId\":\"" + instance.id().value() + "\"}",
                    documentId
            );
            eventPublisher.publish(event);
            notificationService.notifyTaskAssigned("group:reviewers", reviewTask.id(), "REVIEW");
        });

        return instanceWithTasks;
    }

    @Override
    @Transactional
    public WorkflowInstance completeTask(String workflowId, String taskId, String decision, String comments) {
        WorkflowTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.status() == TaskStatus.COMPLETED) {
            log.info("Task already completed: taskId={}, workflowId={}", taskId, workflowId);
            WorkflowInstance current = workflowInstanceRepository.findById(new WorkflowId(workflowId))
                    .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
            return current;
        }

        WorkflowTask completedTask = task.complete("current-user", comments);
        taskRepository.save(completedTask);

        WorkflowInstance instance = workflowInstanceRepository.findById(new WorkflowId(workflowId))
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

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

        WorkflowInstance updated;
        DocumentEvent event;
        if ("APPROVED".equals(decision)) {
            updated = instance.withState(WorkflowState.COMPLETED);
            event = DocumentEvent.create(
                    instance.documentId(),
                    "WORKFLOW_COMPLETED",
                    "{\"decision\":\"APPROVED\"}",
                    instance.correlationId()
            );
        } else {
            updated = instance.withState(WorkflowState.REJECTED);
            event = DocumentEvent.create(
                    instance.documentId(),
                    "WORKFLOW_REJECTED",
                    "{\"decision\":\"REJECTED\"}",
                    instance.correlationId()
            );
        }

        workflowInstanceRepository.save(updated);

        log.info("Workflow {}: taskId={}, completedBy=current-user, decision={}, newState={}",
                updated.state().name(), taskId, decision, updated.state().name());
        workflowMetrics.incrementTaskCompleted();

        auditService.log(AuditLog.create(
                "current-user",
                "TASK_COMPLETED",
                "WORKFLOW_TASK",
                taskId,
                Map.of("workflowId", workflowId, "decision", decision, "newState", updated.state().name())
        ));

        publishEventAsync(() -> {
            eventPublisher.publish(event);
        });

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowInstance getStatus(String workflowId) {
        WorkflowId workflowIdVo = WorkflowId.from(workflowId);
        return workflowInstanceRepository.findById(workflowIdVo)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
    }

    private void publishEventAsync(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Failed to publish async side-effect for workflow operation", e);
        }
    }
}
