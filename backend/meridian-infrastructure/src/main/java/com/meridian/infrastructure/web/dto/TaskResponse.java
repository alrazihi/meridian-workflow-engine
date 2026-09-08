package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.WorkflowTask;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Workflow task response")
public record TaskResponse(
        @Schema(description = "Unique task identifier", example = "task-123")
        String taskId,
        @Schema(description = "Assigned user or group", example = "group:reviewers")
        String assignee,
        @Schema(description = "Required action", example = "REVIEW")
        String action,
        @Schema(description = "Current task status", example = "ASSIGNED")
        String status,
        @Schema(description = "Due date, or null if not set", example = "2024-01-20T10:30:00Z")
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
                task.status().name(),
                task.dueAt()
        );
    }
}
