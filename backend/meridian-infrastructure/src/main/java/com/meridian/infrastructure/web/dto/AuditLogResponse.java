package com.meridian.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Audit log entry (sanitized - no IP or user agent)")
public record AuditLogResponse(
        @Schema(description = "Unique log entry ID", example = "log-123")
        String id,
        @Schema(description = "Actor who performed the action", example = "user@example.com")
        String actor,
        @Schema(description = "Action performed", example = "WORKFLOW_STARTED")
        String action,
        @Schema(description = "Type of resource", example = "WORKFLOW_INSTANCE")
        String resourceType,
        @Schema(description = "Resource identifier", example = "wf-123")
        String resourceId,
        @Schema(description = "When the event occurred", example = "2024-01-15T10:30:00Z")
        Instant occurredAt,
        @Schema(description = "When the log entry was created", example = "2024-01-15T10:30:05Z")
        Instant createdAt
) {
    public static AuditLogResponse from(com.meridian.domain.model.AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new AuditLogResponse(
                auditLog.id(),
                auditLog.actor(),
                auditLog.action(),
                auditLog.resourceType(),
                auditLog.resourceId(),
                auditLog.occurredAt(),
                auditLog.createdAt()
        );
    }
}