package com.meridian.infrastructure.persistence.repository;

import com.meridian.domain.model.DocumentAcl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DocumentAclRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaDocumentAclRepository jpaDocumentAclRepository;

    @Test
    void shouldGrantDocumentAccess() {
        DocumentAcl acl = DocumentAcl.grant("doc-123", "user-456", "READ");

        DocumentAcl saved = jpaDocumentAclRepository.save(acl);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.actor()).isEqualTo("user-456");
        assertThat(saved.permission()).isEqualTo("READ");
    }

    @Test
    void shouldFindAclsByDocumentId() {
        DocumentAcl acl1 = DocumentAcl.grant("doc-123", "user-1", "READ");
        DocumentAcl acl2 = DocumentAcl.grant("doc-123", "user-2", "WRITE");
        jpaDocumentAclRepository.save(acl1);
        jpaDocumentAclRepository.save(acl2);

        List<DocumentAcl> acls = jpaDocumentAclRepository.findByDocumentId("doc-123");

        assertThat(acls).hasSize(2);
        assertThat(acls).extracting(DocumentAcl::actor).containsExactlyInAnyOrder("user-1", "user-2");
    }

    @Test
    void shouldFindAclsByActor() {
        DocumentAcl acl1 = DocumentAcl.grant("doc-123", "user-1", "READ");
        DocumentAcl acl2 = DocumentAcl.grant("doc-456", "user-1", "WRITE");
        jpaDocumentAclRepository.save(acl1);
        jpaDocumentAclRepository.save(acl2);

        List<DocumentAcl> acls = jpaDocumentAclRepository.findByActor("user-1");

        assertThat(acls).hasSize(2);
        assertThat(acls).extracting(DocumentAcl::documentId).containsExactlyInAnyOrder("doc-123", "doc-456");
    }

    @Test
    void shouldFindAclByDocumentIdAndActor() {
        DocumentAcl acl = DocumentAcl.grant("doc-123", "user-1", "READ");
        jpaDocumentAclRepository.save(acl);

        Optional<DocumentAcl> found = jpaDocumentAclRepository.findByDocumentIdAndActor("doc-123", "user-1");

        assertThat(found).isPresent();
        assertThat(found.get().permission()).isEqualTo("READ");
    }

    @Test
    void shouldReturnEmptyWhenAclNotFound() {
        Optional<DocumentAcl> found = jpaDocumentAclRepository.findByDocumentIdAndActor("missing", "missing");
        assertThat(found).isEmpty();
    }
}
