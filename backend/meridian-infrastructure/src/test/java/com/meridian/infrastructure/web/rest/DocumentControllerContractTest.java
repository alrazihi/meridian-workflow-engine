package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class DocumentControllerContractTest {

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
    void shouldReturn201WithDocumentResponseOnSuccessfulIngest() throws Exception {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null);
        document = new Document(
                document.id(), document.contentHash(), document.metadata(),
                DocumentStatus.VALIDATING, document.type(), document.priority(),
                document.createdAt(), document.updatedAt(), document.version(), document.idempotencyKey()
        );
        when(ingestDocumentUseCase.ingest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(document);

        mockMvc.perform(post("/api/v1/documents")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("file", "test")
                .param("type", "INVOICE")
                .param("priority", "NORMAL")
                .param("metadata", "{\"vendorId\":\"VEND-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(document.id().value()))
                .andExpect(jsonPath("$.status").value("VALIDATING"))
                .andExpect(jsonPath("$.type").value("INVOICE"))
                .andExpect(jsonPath("$.priority").value("NORMAL"))
                .andExpect(jsonPath("$.metadata.vendorId").value("VEND-001"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.contentHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturn400ForInvalidMetadataJson() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("file", "test")
                .param("type", "INVOICE")
                .param("metadata", "invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturn200WithPaginatedDocuments() throws Exception {
        Document doc1 = Document.create("hash1", DocumentType.INVOICE, Map.of(), null);
        Document doc2 = Document.create("hash2", DocumentType.RECEIPT, Map.of(), null);
        when(queryDocumentUseCase.listDocuments()).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/v1/documents?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].documentId").value(doc1.id().value()))
                .andExpect(jsonPath("$[0].type").value("INVOICE"))
                .andExpect(jsonPath("$[1].documentId").value(doc2.id().value()))
                .andExpect(jsonPath("$[1].type").value("RECEIPT"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturn400ForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/documents?page=-1&size=200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnDocumentById() throws Exception {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null);
        when(queryDocumentUseCase.getDocument(org.mockito.ArgumentMatchers.any()))
                .thenReturn(document);

        mockMvc.perform(get("/api/v1/documents/doc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(document.id().value()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.type").value("INVOICE"));
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void shouldDenyAccessToListDocumentsForReviewer() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isForbidden());
    }
}
