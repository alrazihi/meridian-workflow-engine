package com.meridian.domain.model.valueobjects;

import java.util.UUID;

public record WorkflowId(String value) {
    public WorkflowId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId cannot be null or blank");
        }
    }

    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID().toString());
    }

    public static WorkflowId from(String value) {
        return new WorkflowId(value);
    }
}
