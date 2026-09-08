package com.meridian.infrastructure.persistence.repository;

import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.application.port.outbound.WorkflowInstanceRepository;
import com.meridian.domain.model.WorkflowInstance;
import com.meridian.domain.model.valueobjects.DocumentId;
import com.meridian.domain.model.valueobjects.WorkflowId;
import com.meridian.infrastructure.persistence.jpa.WorkflowInstanceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaWorkflowInstanceRepository implements WorkflowInstanceRepository {

    private final org.springframework.data.jpa.repository.JpaRepository<WorkflowInstanceEntity, String> workflowInstanceJpaRepository;
    private final TaskRepository taskRepository;

    public JpaWorkflowInstanceRepository(
            org.springframework.data.jpa.repository.JpaRepository<WorkflowInstanceEntity, String> workflowInstanceJpaRepository,
            TaskRepository taskRepository) {
        this.workflowInstanceJpaRepository = workflowInstanceJpaRepository;
        this.taskRepository = taskRepository;
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
        List<com.meridian.domain.model.WorkflowTask> tasks = taskRepository.findByWorkflowId(new WorkflowId(entity.getId()));
        return new WorkflowInstance(
                new WorkflowId(entity.getId()),
                new DocumentId(entity.getDocumentId()),
                entity.getState(),
                entity.getContext(),
                entity.getCorrelationId(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                tasks
        );
    }
}
