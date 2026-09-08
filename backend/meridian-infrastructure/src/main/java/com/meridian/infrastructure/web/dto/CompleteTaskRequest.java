package com.meridian.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to complete a workflow task")
public record CompleteTaskRequest(
        @Schema(description = "Decision: APPROVED or REJECTED", example = "APPROVED", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "decision is required")
        @Pattern(regexp = "APPROVED|REJECTED", message = "decision must be APPROVED or REJECTED")
        String decision,
        @Schema(description = "Optional comments", example = "Looks good", maxLength = 4000)
        @Size(max = 4000, message = "comments must not exceed 4000 characters")
        String comments
) {
}
