package com.meridian.infrastructure.persistence.repository;

import com.meridian.infrastructure.persistence.jpa.DocumentAclEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentAclJpaRepository extends JpaRepository<DocumentAclEntity, String> {
    List<DocumentAclEntity> findByDocumentId(String documentId);
    List<DocumentAclEntity> findByActor(String actor);
    DocumentAclEntity findByDocumentIdAndActor(String documentId, String actor);
}
