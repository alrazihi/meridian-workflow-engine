package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import com.meridian.infrastructure.web.dto.CompleteTaskRequest;
import com.meridian.infrastructure.web.dto.WorkflowStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Workflows", description = "Workflow status and task completion operations")
@RestController
@RequestMapping("/api/v1")
public class WorkflowController {

    private final QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    public WorkflowController(QueryWorkflowStatusUseCase queryWorkflowStatusUseCase, CompleteTaskUseCase completeTaskUseCase) {
        this.queryWorkflowStatusUseCase = queryWorkflowStatusUseCase;
        this.completeTaskUseCase = completeTaskUseCase;
    }

    @Operation(summary = "Get workflow status", description = "Returns the current status of a workflow including active tasks. Requires OPERATOR, REVIEWER, or ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workflow status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Workflow not found", content = @Content)
    })
    @GetMapping("/workflows/{workflowId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('REVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<WorkflowStatusResponse> getWorkflowStatus(@PathVariable String workflowId) {
        WorkflowStatusResponse response = queryWorkflowStatusUseCase.getStatus(workflowId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Complete a workflow task", description = "Approves or rejects a workflow task. Requires REVIEWER role. Idempotent - calling twice returns the same result.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkflowStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Workflow or task not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Task already completed or optimistic lock failure", content = @Content)
    })
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
