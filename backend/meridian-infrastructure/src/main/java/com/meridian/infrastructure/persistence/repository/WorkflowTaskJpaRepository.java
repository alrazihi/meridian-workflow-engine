package com.meridian.infrastructure.persistence.repository;

import com.meridian.infrastructure.persistence.jpa.WorkflowTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTaskJpaRepository extends JpaRepository<WorkflowTaskEntity, String> {
    List<WorkflowTaskEntity> findByWorkflowId(String workflowId);
    List<WorkflowTaskEntity> findByWorkflowIdAndStatus(String workflowId, String status);
}
