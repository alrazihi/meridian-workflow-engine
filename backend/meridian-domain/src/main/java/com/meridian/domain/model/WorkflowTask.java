package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.WorkflowId;

import java.time.Instant;
import java.util.Objects;

public record WorkflowTask(
    String id,
    WorkflowId workflowId,
    String assignee,
    String action,
    TaskStatus status,
    Instant dueAt,
    Instant completedAt,
    String completedBy,
    String comments,
    Instant createdAt,
    long version
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
            TaskStatus.PENDING,
            null,
            null,
            null,
            null,
            Instant.now(),
            0L
        );
    }

    public WorkflowTask assign() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException("Cannot assign task in status: " + status);
        }
        return new WorkflowTask(
            this.id,
            this.workflowId,
            this.assignee,
            this.action,
            TaskStatus.ASSIGNED,
            this.dueAt,
            this.completedAt,
            this.completedBy,
            this.comments,
            this.createdAt,
            this.version
        );
    }

    public WorkflowTask complete(String completedBy, String comments) {
        if (status == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Task is already completed");
        }
        return new WorkflowTask(
            this.id,
            this.workflowId,
            this.assignee,
            this.action,
            TaskStatus.COMPLETED,
            this.dueAt,
            Instant.now(),
            completedBy,
            comments,
            this.createdAt,
            this.version
        );
    }
}
