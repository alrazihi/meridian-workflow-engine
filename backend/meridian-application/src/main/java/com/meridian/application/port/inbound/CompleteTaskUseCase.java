package com.meridian.application.port.inbound;

import com.meridian.domain.model.WorkflowInstance;

public interface CompleteTaskUseCase {
    WorkflowInstance completeTask(String workflowId, String taskId, String decision, String comments);
}
