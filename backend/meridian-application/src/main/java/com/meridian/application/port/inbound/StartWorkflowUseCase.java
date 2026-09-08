package com.meridian.application.port.inbound;

import com.meridian.domain.model.WorkflowInstance;

public interface StartWorkflowUseCase {
    WorkflowInstance start(String documentId);
}
