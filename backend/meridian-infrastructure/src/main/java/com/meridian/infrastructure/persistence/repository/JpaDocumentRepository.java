package com.meridian.infrastructure.persistence.repository;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.infrastructure.persistence.jpa.DocumentEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaDocumentRepository implements DocumentRepository {

    private final DocumentJpaRepository documentJpaRepository;

    public JpaDocumentRepository(DocumentJpaRepository documentJpaRepository) {
        this.documentJpaRepository = documentJpaRepository;
    }

    @Override
    public Document save(Document document) {
        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Document> findById(DocumentId id) {
        return documentJpaRepository.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return documentJpaRepository.existsByIdempotencyKey(idempotencyKey);
    }

    private DocumentEntity toEntity(Document document) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.id().value());
        entity.setContentHash(document.contentHash());
        entity.setMetadata(document.metadata());
        entity.setStatus(document.status().name());
        entity.setType(document.type().name());
        entity.setPriority(document.priority().name());
        entity.setVersion(document.version());
        entity.setIdempotencyKey(document.idempotencyKey());
        return entity;
    }

    private Document toDomain(DocumentEntity entity) {
        return new Document(
                new DocumentId(entity.getId()),
                entity.getContentHash(),
                entity.getMetadata(),
                DocumentStatus.valueOf(entity.getStatus()),
                DocumentType.valueOf(entity.getType()),
                Priority.valueOf(entity.getPriority()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                entity.getIdempotencyKey()
        );
    }
}
