package com.meridian.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standardized error response")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "Error type", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error message", example = "Invalid metadata JSON: unexpected character")
        String message,
        @Schema(description = "ISO-8601 timestamp of the error", example = "2024-01-15T10:30:00Z")
        String timestamp,
        @Schema(description = "Correlation ID for tracing", example = "abc-123")
        String correlationId
) {
}
