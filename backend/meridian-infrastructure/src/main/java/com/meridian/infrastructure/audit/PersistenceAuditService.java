package com.meridian.infrastructure.audit;

import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
import com.meridian.infrastructure.persistence.jpa.AuditLogEntity;
import com.meridian.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class PersistenceAuditService implements AuditService {

    private final AuditLogJpaRepository auditLogJpaRepository;

    public PersistenceAuditService(AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Override
    public void log(AuditLog auditLog) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(auditLog.id());
        entity.setActor(auditLog.actor());
        entity.setAction(auditLog.action());
        entity.setResourceType(auditLog.resourceType());
        entity.setResourceId(auditLog.resourceId());
        entity.setIpAddress(auditLog.ipAddress());
        entity.setUserAgent(auditLog.userAgent());
        if (auditLog.details() != null) {
            entity.setDetails(auditLog.details().toString());
        }
        entity.setOccurredAt(auditLog.occurredAt());
        entity.setCreatedAt(auditLog.createdAt());
        auditLogJpaRepository.save(entity);
    }
}
