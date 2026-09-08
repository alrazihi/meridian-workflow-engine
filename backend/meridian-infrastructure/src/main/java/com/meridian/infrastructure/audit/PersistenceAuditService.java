package com.meridian.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
import com.meridian.infrastructure.persistence.jpa.AuditLogEntity;
import com.meridian.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public List<AuditLog> query(String actor, String resourceType, String resourceId, Instant from, Instant to) {
        if (from == null) from = Instant.now().minusSeconds(86400);
        if (to == null) to = Instant.now();

        List<AuditLogEntity> entities;
        if (actor != null && resourceType != null && resourceId != null) {
            entities = auditLogJpaRepository.findByResourceTypeAndResourceIdAndOccurredAtBetween(resourceType, resourceId, from, to);
        } else if (actor != null) {
            entities = auditLogJpaRepository.findByActorAndOccurredAtBetween(actor, from, to);
        } else {
            entities = auditLogJpaRepository.findAll().stream()
                    .filter(e -> e.getOccurredAt().isAfter(from) && e.getOccurredAt().isBefore(to))
                    .toList();
        }

        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private AuditLog toDomain(AuditLogEntity entity) {
        Map<String, Object> details = Map.of();
        if (entity.getDetails() != null && !entity.getDetails().isBlank()) {
            try {
                details = objectMapper.readValue(entity.getDetails(), Map.class);
            } catch (Exception e) {
                details = Map.of();
            }
        }
        return new AuditLog(
                entity.getId(),
                entity.getActor(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                details,
                entity.getOccurredAt(),
                entity.getCreatedAt()
        );
    }
}
