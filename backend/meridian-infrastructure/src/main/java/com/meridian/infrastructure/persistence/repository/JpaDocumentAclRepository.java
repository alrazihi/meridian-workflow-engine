package com.meridian.infrastructure.persistence.repository;

import com.meridian.application.port.outbound.DocumentAclRepository;
import com.meridian.domain.model.DocumentAcl;
import com.meridian.infrastructure.persistence.jpa.DocumentAclEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaDocumentAclRepository implements DocumentAclRepository {

    private final DocumentAclJpaRepository documentAclJpaRepository;

    public JpaDocumentAclRepository(DocumentAclJpaRepository documentAclJpaRepository) {
        this.documentAclJpaRepository = documentAclJpaRepository;
    }

    @Override
    public DocumentAcl save(DocumentAcl acl) {
        DocumentAclEntity entity = toEntity(acl);
        DocumentAclEntity saved = documentAclJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DocumentAcl> findByDocumentId(String documentId) {
        return documentAclJpaRepository.findByDocumentId(documentId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DocumentAcl> findByActor(String actor) {
        return documentAclJpaRepository.findByActor(actor).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<DocumentAcl> findByDocumentIdAndActor(String documentId, String actor) {
        return documentAclJpaRepository.findByDocumentIdAndActor(documentId, actor)
                .map(this::toDomain);
    }

    private DocumentAclEntity toEntity(DocumentAcl acl) {
        DocumentAclEntity entity = new DocumentAclEntity();
        entity.setId(acl.id());
        entity.setDocumentId(acl.documentId());
        entity.setActor(acl.actor());
        entity.setPermission(acl.permission());
        entity.setGrantedAt(acl.grantedAt());
        return entity;
    }

    private DocumentAcl toDomain(DocumentAclEntity entity) {
        return new DocumentAcl(
                entity.getId(),
                entity.getDocumentId(),
                entity.getActor(),
                entity.getPermission(),
                entity.getGrantedAt()
        );
    }
}