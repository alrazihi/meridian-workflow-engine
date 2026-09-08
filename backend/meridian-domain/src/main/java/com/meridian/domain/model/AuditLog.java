package com.meridian.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLog(
        String id,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        String ipAddress,
        String userAgent,
        Map<String, Object> details,
        Instant occurredAt,
        Instant createdAt
) {
    public static AuditLog create(String actor, String action, String resourceType, String resourceId,
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        return new AuditLog(
                UUID.randomUUID().toString(),
                actor,
                action,
                resourceType,
                resourceId,
                ipAddress,
                userAgent,
                details,
                Instant.now(),
                Instant.now()
        );
    }
}
