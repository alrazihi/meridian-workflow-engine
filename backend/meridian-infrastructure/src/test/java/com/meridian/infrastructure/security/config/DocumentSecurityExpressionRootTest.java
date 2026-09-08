package com.meridian.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSecurityExpressionRootTest {

    @Test
    void shouldAllowAdminAccess() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var root = new DocumentSecurityExpressionRoot(auth);

        assertThat(root.hasDocumentAccess("doc-123")).isTrue();
    }

    @Test
    void shouldAllowOperatorAccess() {
        var auth = new UsernamePasswordAuthenticationToken(
                "operator", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
        var root = new DocumentSecurityExpressionRoot(auth);

        assertThat(root.hasDocumentAccess("doc-123")).isTrue();
    }

    @Test
    void shouldDenyReviewerAccess() {
        var auth = new UsernamePasswordAuthenticationToken(
                "reviewer", null, List.of(new SimpleGrantedAuthority("ROLE_REVIEWER")));
        var root = new DocumentSecurityExpressionRoot(auth);

        assertThat(root.hasDocumentAccess("doc-123")).isFalse();
    }

    @Test
    void shouldDenyUnauthenticatedAccess() {
        var root = new DocumentSecurityExpressionRoot(null);
        assertThat(root.hasDocumentAccess("doc-123")).isFalse();
    }
}
