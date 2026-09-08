package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowState;
import com.meridian.infrastructure.web.dto.CompleteTaskRequest;
import com.meridian.infrastructure.web.dto.WorkflowStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class WorkflowControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;

    @MockBean
    private CompleteTaskUseCase completeTaskUseCase;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturn200WithWorkflowStatus() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"), "corr-456");
        WorkflowStatusResponse response = WorkflowStatusResponse.from(instance);
        when(queryWorkflowStatusUseCase.getStatus("wf-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/workflows/wf-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value("wf-1"))
                .andExpect(jsonPath("$.documentId").value("doc-123"))
                .andExpect(jsonPath("$.state").value("STARTED"));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldCompleteTaskAndReturn200() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"), "corr-456");
        WorkflowStatusResponse response = WorkflowStatusResponse.from(instance.withState(WorkflowState.COMPLETED));
        when(completeTaskUseCase.completeTask("wf-1", "task-1", "APPROVED", "Looks good"))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"Looks good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldReturn400ForInvalidDecision() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"INVALID\",\"comments\":\"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenyTaskCompletionToNonReviewer() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"test\"}"))
                .andExpect(status().isForbidden());
    }
}