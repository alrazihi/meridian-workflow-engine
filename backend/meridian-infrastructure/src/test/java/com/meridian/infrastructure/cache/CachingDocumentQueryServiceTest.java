package com.meridian.infrastructure.cache;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingDocumentQueryServiceTest {

    @Mock
    private QueryDocumentUseCase delegate;

    @InjectMocks
    private CachingDocumentQueryService cachingService;

    @Test
    void shouldCacheDocumentById() {
        Document document = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null);
        when(delegate.getDocument(new DocumentId("doc-123"))).thenReturn(document);

        Document result1 = cachingService.getDocument(new DocumentId("doc-123"));
        Document result2 = cachingService.getDocument(new DocumentId("doc-123"));

        assertThat(result1).isEqualTo(result2);
        verify(delegate).getDocument(new DocumentId("doc-123"));
    }

    @Test
    void shouldReturnNullWhenDelegateReturnsNull() {
        when(delegate.getDocument(new DocumentId("missing"))).thenReturn(null);

        Document result = cachingService.getDocument(new DocumentId("missing"));

        assertThat(result).isNull();
    }
}
