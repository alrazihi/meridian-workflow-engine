package com.meridian.application.port.outbound;

import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.WorkflowId;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    WorkflowTask save(WorkflowTask task);
    List<WorkflowTask> findByWorkflowId(WorkflowId workflowId);
    Optional<WorkflowTask> findById(String taskId);
}
