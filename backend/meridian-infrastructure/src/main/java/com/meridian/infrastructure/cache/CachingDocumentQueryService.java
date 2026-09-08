package com.meridian.infrastructure.cache;

import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CachingDocumentQueryService implements QueryDocumentUseCase {

    private final QueryDocumentUseCase delegate;

    public CachingDocumentQueryService(QueryDocumentUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Cacheable(value = "documents", key = "#documentId.value()")
    public Document getDocument(DocumentId documentId) {
        return delegate.getDocument(documentId);
    }

    @CacheEvict(value = "documents", key = "#documentId.value()")
    public void evictDocument(DocumentId documentId) {
    }
}