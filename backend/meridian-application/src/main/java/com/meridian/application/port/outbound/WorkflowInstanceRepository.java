package com.meridian.application.port.outbound;

import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.valueobjects.WorkflowId;

import java.util.Optional;

public interface WorkflowInstanceRepository {
    WorkflowInstance save(WorkflowInstance workflowInstance);
    Optional<WorkflowInstance> findById(WorkflowId workflowId);
}
