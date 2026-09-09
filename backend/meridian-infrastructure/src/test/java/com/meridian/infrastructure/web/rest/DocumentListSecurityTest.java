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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, MethodSecurityConfig.class})
class DocumentListSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryDocumentUseCase queryDocumentUseCase;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void shouldReturnAllDocumentsWithoutUserFiltering() throws Exception {
        Document doc1 = Document.create("hash1", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null);
        Document doc2 = Document.create("hash2", DocumentType.RECEIPT, Map.of("vendorId", "VEND-002"), null);
        when(queryDocumentUseCase.listDocuments()).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }
}
