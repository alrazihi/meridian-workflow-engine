package com.meridian.application.port.outbound;

import com.meridian.domain.model.AuditLog;

public interface AuditService {
    void log(AuditLog auditLog);
}
