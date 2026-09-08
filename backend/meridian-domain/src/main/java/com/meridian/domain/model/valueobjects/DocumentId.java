package com.meridian.domain.model.valueobjects;

import java.util.UUID;

public record DocumentId(String value) {
    public DocumentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DocumentId cannot be null or blank");
        }
    }

    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID().toString());
    }

    public static DocumentId from(String value) {
        return new DocumentId(value);
    }
}
