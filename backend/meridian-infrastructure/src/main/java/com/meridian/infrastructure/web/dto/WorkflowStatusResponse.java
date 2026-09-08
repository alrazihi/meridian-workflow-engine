package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.WorkflowInstance;

import java.time.Instant;

public record WorkflowStatusResponse(
        String workflowId,
        String documentId,
        String state,
        Instant startedAt,
        Instant completedAt
) {
    public static WorkflowStatusResponse from(WorkflowInstance instance) {
        if (instance == null) {
            return null;
        }
        return new WorkflowStatusResponse(
                instance.id().value(),
                instance.documentId().value(),
                instance.state(),
                instance.startedAt(),
                instance.completedAt()
        );
    }
}
