package com.meridian.domain.service;

import com.meridian.domain.exception.DomainException;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentValidatorTest {

    private final DocumentValidator validator = new DocumentValidator();

    @Test
    void shouldAcceptValidDocument() {
        Document document = Document.create(
                "abc123",
                DocumentType.INVOICE,
                Map.of("vendorId", "VEND-001")
        );

        DocumentValidator.ValidationResult result = validator.validate(document);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldRejectDocumentWithNullContentHash() {
        Document document = new Document(
                com.meridian.domain.model.valueobjects.DocumentId.generate(),
                null,
                Map.of("vendorId", "VEND-001"),
                DocumentStatus.RECEIVED,
                DocumentType.INVOICE,
                com.meridian.domain.model.Priority.NORMAL,
                java.time.Instant.now(),
                java.time.Instant.now(),
                0L
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldAllowTransitionFromReceivedToValidating() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.RECEIVED, DocumentStatus.VALIDATING);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldRejectInvalidTransition() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.RECEIVED, DocumentStatus.COMPLETED);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("Cannot transition");
    }
}
