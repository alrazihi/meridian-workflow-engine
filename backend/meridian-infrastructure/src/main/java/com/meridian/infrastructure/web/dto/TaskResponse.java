package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.WorkflowTask;

import java.time.Instant;

public record TaskResponse(
        String taskId,
        String assignee,
        String action,
        String status,
        Instant dueAt
) {
    public static TaskResponse from(WorkflowTask task) {
        if (task == null) {
            return null;
        }
        return new TaskResponse(
                task.id(),
                task.assignee(),
                task.action(),
                task.status(),
                task.dueAt()
        );
    }
}
