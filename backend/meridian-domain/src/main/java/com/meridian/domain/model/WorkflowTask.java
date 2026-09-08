package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.WorkflowId;

import java.time.Instant;
import java.util.Objects;

public record WorkflowTask(
    String id,
    WorkflowId workflowId,
    String assignee,
    String action,
    String status,
    Instant dueAt,
    Instant completedAt,
    String completedBy,
    String comments,
    Instant createdAt
) {
    public WorkflowTask {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(assignee, "assignee cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
    }

    public static WorkflowTask create(WorkflowId workflowId, String assignee, String action) {
        return new WorkflowTask(
            java.util.UUID.randomUUID().toString(),
            workflowId,
            assignee,
            action,
            "PENDING",
            null,
            null,
            null,
            null,
            Instant.now()
        );
    }

    public WorkflowTask assign() {
        return new WorkflowTask(
            this.id,
            this.workflowId,
            this.assignee,
            this.action,
            "ASSIGNED",
            this.dueAt,
            this.completedAt,
            this.completedBy,
            this.comments,
            this.createdAt
        );
    }

    public WorkflowTask complete(String completedBy, String comments) {
        return new WorkflowTask(
            this.id,
            this.workflowId,
            this.assignee,
            this.action,
            "COMPLETED",
            this.dueAt,
            Instant.now(),
            completedBy,
            comments,
            this.createdAt
        );
    }
}
