package com.meridian.infrastructure.persistence.repository;

import com.meridian.infrastructure.persistence.jpa.DocumentReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentReadJpaRepository extends JpaRepository<DocumentReadEntity, String> {
    List<DocumentReadEntity> findByStatus(String status);
    List<DocumentReadEntity> findByType(String type);
}
