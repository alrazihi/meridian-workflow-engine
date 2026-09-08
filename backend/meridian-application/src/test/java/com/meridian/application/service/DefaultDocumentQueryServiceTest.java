package com.meridian.application.service;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDocumentQueryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DefaultDocumentQueryService queryService;

    @Test
    void shouldReturnDocumentById() {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.findById(new DocumentId("doc-123"))).thenReturn(Optional.of(document));

        Document result = queryService.getDocument(new DocumentId("doc-123"));

        assertThat(result).isEqualTo(document);
        assertThat(result.status()).isEqualTo(DocumentStatus.RECEIVED);
    }

    @Test
    void shouldThrowWhenDocumentNotFound() {
        when(documentRepository.findById(new DocumentId("missing"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getDocument(new DocumentId("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void shouldReturnAllDocuments() {
        List<Document> documents = List.of(
                Document.create("hash1", DocumentType.INVOICE, Map.of(), null, "test-tenant"),
                Document.create("hash2", DocumentType.RECEIPT, Map.of(), null, "test-tenant")
        );
        when(documentRepository.findAll()).thenReturn(documents);

        List<Document> result = queryService.listDocuments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(DocumentType.INVOICE);
        assertThat(result.get(1).type()).isEqualTo(DocumentType.RECEIPT);
    }

    @Test
    void shouldReturnEmptyListWhenNoDocuments() {
        when(documentRepository.findAll()).thenReturn(List.of());

        List<Document> result = queryService.listDocuments();

        assertThat(result).isEmpty();
    }
}
