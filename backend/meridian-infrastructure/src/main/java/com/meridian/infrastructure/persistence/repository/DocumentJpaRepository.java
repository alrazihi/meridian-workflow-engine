package com.meridian.infrastructure.persistence.repository;

import com.meridian.infrastructure.persistence.jpa.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, String> {
    Optional<DocumentEntity> findById(String id);
    boolean existsById(String id);
}
