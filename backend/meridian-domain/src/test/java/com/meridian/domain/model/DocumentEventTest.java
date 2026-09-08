package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.DocumentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentEventTest {

    @Test
    void shouldCreateEvent() {
        DocumentEvent event = DocumentEvent.create(
                new DocumentId("doc-123"),
                "DOCUMENT_CREATED",
                "{}",
                "corr-456"
        );

        assertThat(event.id()).isNotNull();
        assertThat(event.eventType()).isEqualTo("DOCUMENT_CREATED");
        assertThat(event.documentId().value()).isEqualTo("doc-123");
        assertThat(event.correlationId()).isEqualTo("corr-456");
    }

    @Test
    void shouldSetCausationId() {
        DocumentEvent event = DocumentEvent.create(
                new DocumentId("doc-123"),
                "DOCUMENT_VALIDATED",
                "{}",
                "corr-456"
        );

        DocumentEvent withCausation = event.withCausation("event-789");
        assertThat(withCausation.causationId()).isEqualTo("event-789");
    }
}
