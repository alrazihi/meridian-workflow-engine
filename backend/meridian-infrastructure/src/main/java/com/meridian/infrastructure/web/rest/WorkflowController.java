package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import com.meridian.infrastructure.web.dto.CompleteTaskRequest;
import com.meridian.infrastructure.web.dto.WorkflowStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {

    private final QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    public WorkflowController(QueryWorkflowStatusUseCase queryWorkflowStatusUseCase, CompleteTaskUseCase completeTaskUseCase) {
        this.queryWorkflowStatusUseCase = queryWorkflowStatusUseCase;
        this.completeTaskUseCase = completeTaskUseCase;
    }

    @GetMapping("/workflows/{workflowId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('REVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowStatus(@PathVariable String workflowId) {
        WorkflowStatusResponse response = queryWorkflowStatusUseCase.getStatus(workflowId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/workflows/{workflowId}/tasks/{taskId}/complete")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<WorkflowStatusResponse> completeTask(
            @PathVariable String workflowId,
            @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskRequest request
    ) {
        WorkflowStatusResponse response = completeTaskUseCase.completeTask(workflowId, taskId, request.decision(), request.comments());
        return ResponseEntity.ok(response);
    }
}
