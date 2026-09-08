package com.meridian.application.port.outbound;

import com.meridian.domain.model.DocumentAcl;

import java.util.List;
import java.util.Optional;

public interface DocumentAclRepository {
    DocumentAcl save(DocumentAcl acl);
    List<DocumentAcl> findByDocumentId(String documentId);
    List<DocumentAcl> findByActor(String actor);
    Optional<DocumentAcl> findByDocumentIdAndActor(String documentId, String actor);
}
