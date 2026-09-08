package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.infrastructure.web.dto.CompleteTaskRequest;
import com.meridian.infrastructure.web.dto.WorkflowStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;

    @Mock
    private CompleteTaskUseCase completeTaskUseCase;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnWorkflowStatus() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"), "corr-456");
        when(queryWorkflowStatusUseCase.getStatus("wf-1")).thenReturn(instance);

        mockMvc.perform(get("/api/v1/workflows/wf-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowId").value(is(instance.id().value())))
                .andExpect(jsonPath("$.state").value(is("STARTED")));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldCompleteTask() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"), "corr-456");
        when(completeTaskUseCase.completeTask("wf-1", "task-1", "APPROVED", "Looks good"))
                .thenReturn(instance.withState("COMPLETED"));

        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"Looks good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(is("STARTED")));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldDenyTaskCompletionToNonReviewer() throws Exception {
        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"Looks good\"}"))
                .andExpect(status().isForbidden());
    }
}
