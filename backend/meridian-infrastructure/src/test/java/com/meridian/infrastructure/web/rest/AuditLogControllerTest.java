package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAuditLogsFilteredByActor() throws Exception {
        AuditLog log1 = AuditLog.create("user-1", "ACCESS", "DOCUMENT", "doc-1", "127.0.0.1", "curl", Map.of());
        AuditLog log2 = AuditLog.create("user-2", "ACCESS", "DOCUMENT", "doc-2", "127.0.0.1", "curl", Map.of());

        when(auditService.query("user-1", null, null, null, null))
                .thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/audit")
                .param("actor", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].actor").value(is("user-1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAuditLogsFilteredByResource() throws Exception {
        AuditLog log1 = AuditLog.create("user-1", "ACCESS", "DOCUMENT", "doc-1", "127.0.0.1", "curl", Map.of());

        when(auditService.query(null, "DOCUMENT", "doc-1", null, null))
                .thenReturn(List.of(log1));

        mockMvc.perform(get("/api/v1/audit")
                .param("resourceType", "DOCUMENT")
                .param("resourceId", "doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resourceId").value(is("doc-1")));
    }
}
