package com.meridian.infrastructure.security.config;

import com.meridian.application.port.outbound.DocumentAclRepository;
import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.domain.model.DocumentAcl;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.List;

public class DocumentSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final DocumentAclRepository documentAclRepository;
    private final TaskRepository taskRepository;
    private Object filterObject;
    private Object returnObject;

    public DocumentSecurityExpressionRoot(Authentication authentication, DocumentAclRepository documentAclRepository, TaskRepository taskRepository) {
        super(authentication);
        this.documentAclRepository = documentAclRepository;
        this.taskRepository = taskRepository;
    }

    public boolean hasDocumentAccess(String documentId) {
        if (getAuthentication() == null || !getAuthentication().isAuthenticated()) {
            return false;
        }

        var authorities = getAuthentication().getAuthorities();
        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }

        String username = getAuthentication().getName();

        List<DocumentAcl> acls = documentAclRepository.findByDocumentId(documentId);
        boolean hasAcl = acls.stream()
                .anyMatch(acl -> acl.actor().equals(username));
        if (hasAcl) {
            return true;
        }

        boolean isAssignee = taskRepository.findByWorkflowId(new com.meridian.domain.model.valueobjects.WorkflowId(documentId)).stream()
                .anyMatch(task -> task.assignee().equals(username));
        return isAssignee;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getThis() {
        return this;
    }
}
