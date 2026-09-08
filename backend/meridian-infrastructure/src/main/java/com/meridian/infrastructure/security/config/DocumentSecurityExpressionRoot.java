package com.meridian.infrastructure.security.config;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.valueobjects.DocumentId;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public class DocumentSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private Object filterObject;
    private Object returnObject;

    public DocumentSecurityExpressionRoot(Authentication authentication, DocumentRepository documentRepository, TaskRepository taskRepository) {
        super(authentication);
        this.documentRepository = documentRepository;
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

        Optional<Document> document = documentRepository.findById(new DocumentId(documentId));
        if (document.isEmpty()) {
            return false;
        }

        String username = getAuthentication().getName();
        boolean isAssignee = taskRepository.findByWorkflowId(document.get().id()).stream()
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
