package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class ErrorHandlingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryDocumentUseCase queryDocumentUseCase;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldNotLeakInternalDetailsOnDocumentNotFound() throws Exception {
        when(queryDocumentUseCase.getDocument(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalArgumentException("Document not found: doc-123456789"));

        mockMvc.perform(get("/api/v1/documents/doc-123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("doc-123456789"))));
    }
}
