package com.meridian.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.DocumentReadRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.model.Priority;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.infrastructure.persistence.jpa.DocumentReadEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class JpaDocumentReadRepository implements DocumentReadRepository {

    private final DocumentReadJpaRepository documentReadJpaRepository;
    private final ObjectMapper objectMapper;

    public JpaDocumentReadRepository(DocumentReadJpaRepository documentReadJpaRepository, ObjectMapper objectMapper) {
        this.documentReadJpaRepository = documentReadJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(Document document) {
        DocumentReadEntity entity = toEntity(document);
        documentReadJpaRepository.save(entity);
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return documentReadJpaRepository.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Document> findByType(DocumentType type) {
        return documentReadJpaRepository.findByType(type.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Document updateStatus(com.meridian.domain.model.valueobjects.DocumentId id, DocumentStatus newStatus) {
        DocumentReadEntity entity = documentReadJpaRepository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        DocumentStatus currentStatus = DocumentStatus.valueOf(entity.getStatus());
        if (currentStatus == newStatus) {
            return toDomain(entity);
        }

        entity.setStatus(newStatus.name());
        DocumentReadEntity saved = documentReadJpaRepository.save(entity);
        return toDomain(saved);
    }

    private DocumentReadEntity toEntity(Document document) {
        DocumentReadEntity entity = new DocumentReadEntity();
        entity.setId(document.id().value());
        try {
            entity.setMetadata(objectMapper.writeValueAsString(document.metadata()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata", e);
        }
        entity.setContentHash(document.contentHash());
        entity.setStatus(document.status().name());
        entity.setType(document.type().name());
        entity.setPriority(document.priority().name());
        entity.setVersion(document.version());
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Document toDomain(DocumentReadEntity entity) {
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
                null
        );
    }
}