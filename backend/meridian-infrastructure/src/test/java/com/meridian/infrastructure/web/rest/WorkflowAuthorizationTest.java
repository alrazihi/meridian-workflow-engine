package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.infrastructure.web.dto.CompleteTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class WorkflowAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompleteTaskUseCase completeTaskUseCase;

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldAllowAnyReviewerToCompleteAnyTask() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"), "corr-456");
        when(completeTaskUseCase.completeTask("wf-1", "task-1", "APPROVED", "Looks good"))
                .thenReturn(instance.withState("COMPLETED"));

        mockMvc.perform(post("/api/v1/workflows/wf-1/tasks/task-1/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"Looks good\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldAllowReviewerToCompleteTaskForAnotherReviewersWorkflow() throws Exception {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-456"), "corr-789");
        when(completeTaskUseCase.completeTask("wf-other", "task-other", "APPROVED", "test"))
                .thenReturn(instance.withState("COMPLETED"));

        mockMvc.perform(post("/api/v1/workflows/wf-other/tasks/task-other/complete")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"comments\":\"test\"}"))
                .andExpect(status().isOk());
    }
}
