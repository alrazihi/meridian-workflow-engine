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
                Map.of("vendorId", "VEND-001"),
                null,
                "test-tenant"
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
                0L,
                null
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRejectDocumentWithBlankContentHash() {
        Document document = new Document(
                com.meridian.domain.model.valueobjects.DocumentId.generate(),
                "   ",
                Map.of("vendorId", "VEND-001"),
                DocumentStatus.RECEIVED,
                DocumentType.INVOICE,
                com.meridian.domain.model.Priority.NORMAL,
                java.time.Instant.now(),
                java.time.Instant.now(),
                0L,
                null
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRejectDocumentWithNullMetadata() {
        Document document = new Document(
                com.meridian.domain.model.valueobjects.DocumentId.generate(),
                "abc123",
                null,
                DocumentStatus.RECEIVED,
                DocumentType.INVOICE,
                com.meridian.domain.model.Priority.NORMAL,
                java.time.Instant.now(),
                java.time.Instant.now(),
                0L,
                null
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRejectDocumentWithEmptyMetadata() {
        Document document = new Document(
                com.meridian.domain.model.valueobjects.DocumentId.generate(),
                "abc123",
                Map.of(),
                DocumentStatus.RECEIVED,
                DocumentType.INVOICE,
                com.meridian.domain.model.Priority.NORMAL,
                java.time.Instant.now(),
                java.time.Instant.now(),
                0L,
                null
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
    void shouldAllowTransitionFromReceivedToRejected() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.RECEIVED, DocumentStatus.REJECTED);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldRejectInvalidTransitionFromReceivedToCompleted() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.RECEIVED, DocumentStatus.COMPLETED);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("Cannot transition");
    }

    @Test
    void shouldAllowTransitionFromValidatingToRouted() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.VALIDATING, DocumentStatus.ROUTED);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldAllowTransitionFromProcessingToCompleted() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.PROCESSING, DocumentStatus.COMPLETED);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldRejectTransitionFromCompletedToReceived() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.COMPLETED, DocumentStatus.RECEIVED);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void shouldRejectTransitionFromRejectedToAny() {
        assertThat(validator.validateTransition(DocumentStatus.REJECTED, DocumentStatus.ROUTED).isValid()).isFalse();
        assertThat(validator.validateTransition(DocumentStatus.REJECTED, DocumentStatus.COMPLETED).isValid()).isFalse();
    }

    @Test
    void shouldRejectTransitionFromArchivedToAny() {
        assertThat(validator.validateTransition(DocumentStatus.ARCHIVED, DocumentStatus.ROUTED).isValid()).isFalse();
        assertThat(validator.validateTransition(DocumentStatus.ARCHIVED, DocumentStatus.COMPLETED).isValid()).isFalse();
    }

    @Test
    void shouldAllowSameStatusTransition() {
        DocumentValidator.ValidationResult result = validator
                .validateTransition(DocumentStatus.RECEIVED, DocumentStatus.RECEIVED);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void shouldRejectNullFromStatus() {
        assertThatThrownBy(() -> validator.validateTransition(null, DocumentStatus.ROUTED))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullToStatus() {
        assertThatThrownBy(() -> validator.validateTransition(DocumentStatus.RECEIVED, null))
                .isInstanceOf(NullPointerException.class);
    }
}
