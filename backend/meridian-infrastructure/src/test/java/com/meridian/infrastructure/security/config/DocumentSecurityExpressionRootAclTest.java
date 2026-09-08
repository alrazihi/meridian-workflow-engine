package com.meridian.infrastructure.security.config;

import com.meridian.application.port.outbound.DocumentAclRepository;
import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.domain.model.DocumentAcl;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.WorkflowId;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSecurityExpressionRootAclTest {

    @Test
    void shouldAllowAccessWhenUserHasAcl() {
        DocumentAclRepository aclRepo = new DocumentAclRepository() {
            @Override
            public DocumentAcl save(DocumentAcl acl) { return acl; }
            @Override
            public List<DocumentAcl> findByDocumentId(String documentId) {
                return List.of(DocumentAcl.grant("doc-123", "user-1", "READ"));
            }
            @Override
            public List<DocumentAcl> findByActor(String actor) { return List.of(); }
            @Override
            public Optional<DocumentAcl> findByDocumentIdAndActor(String documentId, String actor) {
                return Optional.of(DocumentAcl.grant("doc-123", "user-1", "READ"));
            }
        };

        TaskRepository taskRepo = new TaskRepository() {
            @Override
            public WorkflowTask save(WorkflowTask task) { return task; }
            @Override
            public List<WorkflowTask> findByWorkflowId(WorkflowId workflowId) { return List.of(); }
            @Override
            public Optional<WorkflowTask> findById(String taskId) { return Optional.empty(); }
        };

        var auth = new UsernamePasswordAuthenticationToken(
                "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        var root = new DocumentSecurityExpressionRoot(auth, aclRepo, taskRepo);

        assertThat(root.hasDocumentAccess("doc-123")).isTrue();
    }

    @Test
    void shouldDenyAccessWhenUserHasNoAclAndIsNotAssignee() {
        DocumentAclRepository aclRepo = new DocumentAclRepository() {
            @Override
            public DocumentAcl save(DocumentAcl acl) { return acl; }
            @Override
            public List<DocumentAcl> findByDocumentId(String documentId) { return List.of(); }
            @Override
            public List<DocumentAcl> findByActor(String actor) { return List.of(); }
            @Override
            public Optional<DocumentAcl> findByDocumentIdAndActor(String documentId, String actor) {
                return Optional.empty();
            }
        };

        TaskRepository taskRepo = new TaskRepository() {
            @Override
            public WorkflowTask save(WorkflowTask task) { return task; }
            @Override
            public List<WorkflowTask> findByWorkflowId(WorkflowId workflowId) { return List.of(); }
            @Override
            public Optional<WorkflowTask> findById(String taskId) { return Optional.empty(); }
        };

        var auth = new UsernamePasswordAuthenticationToken(
                "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_REVIEWER")));
        var root = new DocumentSecurityExpressionRoot(auth, aclRepo, taskRepo);

        assertThat(root.hasDocumentAccess("doc-123")).isFalse();
    }
}
