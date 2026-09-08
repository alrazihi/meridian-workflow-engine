package com.meridian.infrastructure.persistence.repository;

import com.meridian.application.port.outbound.WorkflowInstanceRepository;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.valueobjects.WorkflowId;
import com.meridian.infrastructure.persistence.jpa.WorkflowInstanceEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaWorkflowInstanceRepository implements WorkflowInstanceRepository {

    private final org.springframework.data.jpa.repository.JpaRepository<WorkflowInstanceEntity, String> workflowInstanceJpaRepository;

    public JpaWorkflowInstanceRepository(org.springframework.data.jpa.repository.JpaRepository<WorkflowInstanceEntity, String> workflowInstanceJpaRepository) {
        this.workflowInstanceJpaRepository = workflowInstanceJpaRepository;
    }

    @Override
    public WorkflowInstance save(WorkflowInstance workflowInstance) {
        WorkflowInstanceEntity entity = toEntity(workflowInstance);
        WorkflowInstanceEntity saved = workflowInstanceJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<WorkflowInstance> findById(WorkflowId workflowId) {
        return workflowInstanceJpaRepository.findById(workflowId.value())
                .map(this::toDomain);
    }

    private WorkflowInstanceEntity toEntity(WorkflowInstance instance) {
        WorkflowInstanceEntity entity = new WorkflowInstanceEntity();
        entity.setId(instance.id().value());
        entity.setDocumentId(instance.documentId().value());
        entity.setState(instance.state());
        entity.setContext(instance.context());
        entity.setCorrelationId(instance.correlationId());
        entity.setStartedAt(instance.startedAt());
        entity.setCompletedAt(instance.completedAt());
        entity.setVersion(instance.version());
        entity.setCreatedAt(instance.createdAt());
        return entity;
    }

    private WorkflowInstance toDomain(WorkflowInstanceEntity entity) {
        return new WorkflowInstance(
                new WorkflowId(entity.getId()),
                new com.meridian.domain.model.valueobjects.DocumentId(entity.getDocumentId()),
                entity.getState(),
                entity.getContext(),
                entity.getCorrelationId(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                List.of()
        );
    }
}
