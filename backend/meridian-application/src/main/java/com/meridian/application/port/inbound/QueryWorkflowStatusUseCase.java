package com.meridian.application.port.inbound;

import com.meridian.domain.model.WorkflowInstance;

public interface QueryWorkflowStatusUseCase {
    WorkflowInstance getStatus(String workflowId);
}
