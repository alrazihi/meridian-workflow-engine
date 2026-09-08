package com.meridian.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {
    List<AuditLogEntity> findByActorAndOccurredAtBetween(String actor, Instant from, Instant to);
    List<AuditLogEntity> findByResourceTypeAndResourceIdAndOccurredAtBetween(String resourceType, String resourceId, Instant from, Instant to);
}
