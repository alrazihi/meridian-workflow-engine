package com.meridian.infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.infrastructure.web.dto.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IngestDocumentUseCase ingestDocumentUseCase;

    @MockBean
    private QueryDocumentUseCase queryDocumentUseCase;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnDocumentForAuthorizedUser() throws Exception {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null);
        when(queryDocumentUseCase.getDocument(new com.meridian.domain.model.valueobjects.DocumentId("doc-123")))
                .thenReturn(document);

        mockMvc.perform(get("/api/v1/documents/doc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(is(document.id().value())))
                .andExpect(jsonPath("$.status").value(is("RECEIVED")))
                .andExpect(jsonPath("$.type").value(is("INVOICE")));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldDenyAccessToDocumentForUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/v1/documents/doc-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnDocumentList() throws Exception {
        Document doc1 = Document.create("hash1", DocumentType.INVOICE, Map.of(), null);
        Document doc2 = Document.create("hash2", DocumentType.RECEIPT, Map.of(), null);
        when(queryDocumentUseCase.listDocuments()).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value(is("INVOICE")))
                .andExpect(jsonPath("$[1].type").value(is("RECEIPT")));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldRejectIngestWithInvalidMetadata() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("file", "test")
                .param("type", "INVOICE")
                .param("metadata", "invalid-json"))
                .andExpect(status().isBadRequest());
    }
}
