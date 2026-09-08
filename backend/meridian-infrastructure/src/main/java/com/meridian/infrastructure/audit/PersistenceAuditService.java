package com.meridian.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public PersistenceAuditService(AuditLogJpaRepository auditLogJpaRepository, ObjectMapper objectMapper) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.objectMapper = objectMapper;
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
            try {
                entity.setDetails(objectMapper.writeValueAsString(auditLog.details()));
            } catch (JsonProcessingException e) {
                entity.setDetails("{}");
            }
        }
        entity.setOccurredAt(auditLog.occurredAt());
        entity.setCreatedAt(auditLog.createdAt());
        auditLogJpaRepository.save(entity);
    }
}
