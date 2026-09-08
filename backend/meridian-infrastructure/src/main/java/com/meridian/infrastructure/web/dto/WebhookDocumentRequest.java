package com.meridian.infrastructure.web.dto;

import com.meridian.domain.model.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

@Schema(description = "Webhook document ingestion request")
public record WebhookDocumentRequest(
        @Schema(description = "Base64 or raw document content", example = "dGVzdCBjb250ZW50", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "content is required")
        String content,
        @Schema(description = "Document type", example = "INVOICE", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED, allowableValues = {"INVOICE", "RECEIPT", "CONTRACT", "REPORT"})
        @NotBlank(message = "type is required")
        @Pattern(regexp = "INVOICE|RECEIPT|CONTRACT|REPORT", message = "type must be INVOICE, RECEIPT, CONTRACT, or REPORT")
        String type,
        @Schema(description = "Processing priority", example = "NORMAL", allowableValues = {"NORMAL", "HIGH", "LOW"})
        String priority,
        @Schema(description = "Optional metadata key-value pairs")
        Map<String, Object> metadata,
        @Schema(description = "Optional idempotency key for deduplication", example = "my-unique-key-123")
        String idempotencyKey
) {
}