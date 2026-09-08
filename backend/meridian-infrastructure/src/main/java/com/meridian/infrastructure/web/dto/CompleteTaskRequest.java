package com.meridian.infrastructure.web.dto;

public record CompleteTaskRequest(
        String decision,
        String comments
) {
}
