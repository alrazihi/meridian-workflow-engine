package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.TaskStatus;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;

import java.time.Instant;

public record WorkflowStatusResponse(
        String workflowId,
        String documentId,
        String state,
        TaskResponse currentTask,
        Instant startedAt,
        Instant completedAt
) {
    public static WorkflowStatusResponse from(WorkflowInstance instance) {
        if (instance == null) {
            return null;
        }
        WorkflowTask currentTask = instance.tasks().stream()
                .filter(t -> t.status() != TaskStatus.COMPLETED)
                .findFirst()
                .orElse(null);
        return new WorkflowStatusResponse(
                instance.id().value(),
                instance.documentId().value(),
                instance.state(),
                currentTask != null ? TaskResponse.from(currentTask) : null,
                instance.startedAt(),
                instance.completedAt()
        );
    }
}
