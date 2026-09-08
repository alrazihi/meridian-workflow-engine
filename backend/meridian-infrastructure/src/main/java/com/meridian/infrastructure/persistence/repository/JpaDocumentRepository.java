package com.meridian.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.infrastructure.persistence.jpa.DocumentEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class JpaDocumentRepository implements DocumentRepository {

    private final DocumentJpaRepository documentJpaRepository;
    private final ObjectMapper objectMapper;

    public JpaDocumentRepository(DocumentJpaRepository documentJpaRepository, ObjectMapper objectMapper) {
        this.documentJpaRepository = documentJpaRepository;
        this.objectMapper = objectMapper;
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
    public List<Document> findAll() {
        return documentJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return documentJpaRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Document updateStatus(DocumentId id, DocumentStatus newStatus) {
        DocumentEntity entity = documentJpaRepository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentStatus currentStatus = DocumentStatus.valueOf(entity.getStatus());
        if (currentStatus == newStatus) {
            return toDomain(entity);
        }

        entity.setStatus(newStatus.name());
        DocumentEntity saved = documentJpaRepository.save(entity);
        return toDomain(saved);
    }

    private DocumentEntity toEntity(Document document) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(document.id().value());
        entity.setContentHash(document.contentHash());
        try {
            entity.setMetadata(objectMapper.writeValueAsString(document.metadata()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata", e);
        }
        entity.setStatus(document.status().name());
        entity.setType(document.type().name());
        entity.setPriority(document.priority().name());
        entity.setVersion(document.version());
        entity.setIdempotencyKey(document.idempotencyKey());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Document toDomain(DocumentEntity entity) {
        Map<String, String> metadata = Map.of();
        if (entity.getMetadata() != null && !entity.getMetadata().isBlank()) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize metadata", e);
            }
        }
        return new Document(
                new DocumentId(entity.getId()),
                entity.getContentHash(),
                metadata,
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
