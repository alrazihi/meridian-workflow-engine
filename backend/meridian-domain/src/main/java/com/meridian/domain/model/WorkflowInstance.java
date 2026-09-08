package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.valueobjects.WorkflowId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WorkflowInstance(
    WorkflowId id,
    DocumentId documentId,
    WorkflowState state,
    String context,
    String correlationId,
    Instant startedAt,
    Instant completedAt,
    long version,
    Instant createdAt,
    List<WorkflowTask> tasks
) {
    public WorkflowInstance {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(documentId, "documentId cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId cannot be null or blank");
        }
    }

    public static WorkflowInstance start(DocumentId documentId, String correlationId) {
        return new WorkflowInstance(
            WorkflowId.generate(),
            documentId,
            WorkflowState.STARTED,
            "{}",
            correlationId,
            Instant.now(),
            null,
            0L,
            Instant.now(),
            List.of()
        );
    }

    public List<WorkflowTask> tasks() {
        return tasks;
    }

    public WorkflowInstance withState(WorkflowState newState) {
        return new WorkflowInstance(
            this.id,
            this.documentId(),
            newState,
            this.context(),
            this.correlationId(),
            this.startedAt(),
            this.completedAt(),
            this.version + 1,
            this.createdAt(),
            this.tasks
        );
    }
}