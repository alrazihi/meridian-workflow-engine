package com.meridian.infrastructure.security.config;

import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class DocumentSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private Object filterObject;
    private Object returnObject;

    public DocumentSecurityExpressionRoot(Authentication authentication) {
        super(authentication);
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

        boolean isOperator = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR"));
        if (isOperator) {
            return true;
        }

        return false;
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
