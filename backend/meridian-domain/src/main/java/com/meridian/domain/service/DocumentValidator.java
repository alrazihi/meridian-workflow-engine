package com.meridian.domain.service;

import com.meridian.domain.exception.DomainException;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentStatus;

import java.util.Objects;

public class DocumentValidator {

    public ValidationResult validate(Document document) {
        Objects.requireNonNull(document, "document cannot be null");

        if (document.contentHash() == null || document.contentHash().isBlank()) {
            return ValidationResult.invalid("Document content hash is missing");
        }

        if (document.metadata() == null || document.metadata().isEmpty()) {
            return ValidationResult.invalid("Document metadata is required");
        }

        return ValidationResult.valid();
    }

    public ValidationResult validateTransition(DocumentStatus from, DocumentStatus to) {
        Objects.requireNonNull(from, "from status cannot be null");
        Objects.requireNonNull(to, "to status cannot be null");

        if (from == to) {
            return ValidationResult.valid();
        }

        return switch (from) {
            case RECEIVED -> {
                if (to == DocumentStatus.VALIDATING || to == DocumentStatus.REJECTED) {
                    yield ValidationResult.valid();
                }
                yield ValidationResult.invalid("Cannot transition from RECEIVED to " + to);
            }
            case VALIDATING -> {
                if (to == DocumentStatus.ROUTED || to == DocumentStatus.REJECTED) {
                    yield ValidationResult.valid();
                }
                yield ValidationResult.invalid("Cannot transition from VALIDATING to " + to);
            }
            case ROUTED -> {
                if (to == DocumentStatus.PROCESSING || to == DocumentStatus.REJECTED) {
                    yield ValidationResult.valid();
                }
                yield ValidationResult.invalid("Cannot transition from ROUTED to " + to);
            }
            case PROCESSING -> {
                if (to == DocumentStatus.COMPLETED || to == DocumentStatus.REJECTED) {
                    yield ValidationResult.valid();
                }
                yield ValidationResult.invalid("Cannot transition from PROCESSING to " + to);
            }
            case COMPLETED -> {
                if (to == DocumentStatus.ARCHIVED) {
                    yield ValidationResult.valid();
                }
                yield ValidationResult.invalid("Cannot transition from COMPLETED to " + to);
            }
            case REJECTED, ARCHIVED -> {
                yield ValidationResult.invalid("Cannot transition from " + from + " to " + to);
            }
        };
    }

    public record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String errorMessage() {
            return errorMessage;
        }
    }
}
