package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.TaskStatus;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.domain.model.WorkflowTask;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Workflow status response")
public record WorkflowStatusResponse(
        @Schema(description = "Unique workflow identifier", example = "wf-123")
        String workflowId,
        @Schema(description = "Associated document identifier", example = "doc-456")
        String documentId,
        @Schema(description = "Current workflow state", example = "STARTED")
        WorkflowState state,
        @Schema(description = "Current active task, or null if workflow is completed")
        TaskResponse currentTask,
        @Schema(description = "When the workflow was started", example = "2024-01-15T10:30:00Z")
        Instant startedAt,
        @Schema(description = "When the workflow was completed, or null if still active", example = "2024-01-15T10:35:00Z")
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
