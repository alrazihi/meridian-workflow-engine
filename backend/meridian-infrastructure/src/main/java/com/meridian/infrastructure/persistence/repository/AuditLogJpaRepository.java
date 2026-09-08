package com.meridian.infrastructure.persistence.repository;

import com.meridian.infrastructure.persistence.jpa.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {
}
