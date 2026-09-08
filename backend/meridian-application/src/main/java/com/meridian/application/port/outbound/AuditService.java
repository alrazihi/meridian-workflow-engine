package com.meridian.application.port.outbound;

import com.meridian.domain.model.AuditLog;

import java.time.Instant;
import java.util.List;

public interface AuditService {
    void log(AuditLog auditLog);
    List<AuditLog> query(String actor, String resourceType, String resourceId, Instant from, Instant to);
}
