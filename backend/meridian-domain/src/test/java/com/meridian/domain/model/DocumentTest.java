package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void shouldCreateDocument() {
        Document document = Document.create(
                "hash123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001"),
                null,
                "test-tenant"
        );

        assertThat(document.id()).isNotNull();
        assertThat(document.status()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(document.type()).isEqualTo(DocumentType.INVOICE);
        assertThat(document.priority()).isEqualTo(Priority.NORMAL);
        assertThat(document.version()).isEqualTo(0L);
    }

    @Test
    void shouldRejectDocumentWithNullId() {
        assertThatThrownBy(() -> new Document(
                null,
                "hash",
                Map.of(),
                DocumentStatus.RECEIVED,
                DocumentType.INVOICE,
                Priority.NORMAL,
                Instant.now(),
                Instant.now(),
                0L,
                null
        )).isInstanceOf(NullPointerException.class);
    }
}
