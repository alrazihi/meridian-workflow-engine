package com.meridian.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIdTest {

    @Test
    void shouldGenerateUniqueDocumentId() {
        DocumentId id1 = DocumentId.generate();
        DocumentId id2 = DocumentId.generate();

        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    void shouldRejectNullDocumentId() {
        assertThatThrownBy(() -> new DocumentId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankDocumentId() {
        assertThatThrownBy(() -> new DocumentId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateDocumentIdFromString() {
        DocumentId id = DocumentId.from("doc-123");

        assertThat(id.value()).isEqualTo("doc-123");
    }
}
