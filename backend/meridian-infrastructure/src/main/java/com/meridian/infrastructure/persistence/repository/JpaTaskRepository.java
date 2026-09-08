package com.meridian.infrastructure.persistence.repository;

import com.meridian.application.port.outbound.TaskRepository;
import com.meridian.domain.model.WorkflowTask;
import com.meridian.domain.model.valueobjects.WorkflowId;
import com.meridian.infrastructure.persistence.jpa.WorkflowTaskEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaTaskRepository implements TaskRepository {

    private final WorkflowTaskJpaRepository workflowTaskJpaRepository;

    public JpaTaskRepository(WorkflowTaskJpaRepository workflowTaskJpaRepository) {
        this.workflowTaskJpaRepository = workflowTaskJpaRepository;
    }

    @Override
    public WorkflowTask save(WorkflowTask task) {
        WorkflowTaskEntity entity = toEntity(task);
        WorkflowTaskEntity saved = workflowTaskJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<WorkflowTask> findByWorkflowId(WorkflowId workflowId) {
        return workflowTaskJpaRepository.findByWorkflowId(workflowId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<WorkflowTask> findById(String taskId) {
        return workflowTaskJpaRepository.findById(taskId)
                .map(this::toDomain);
    }

    private WorkflowTaskEntity toEntity(WorkflowTask task) {
        WorkflowTaskEntity entity = new WorkflowTaskEntity();
        entity.setId(task.id());
        entity.setWorkflowId(task.workflowId().value());
        entity.setAssignee(task.assignee());
        entity.setAction(task.action());
        entity.setStatus(task.status());
        entity.setDueAt(task.dueAt());
        entity.setCompletedAt(task.completedAt());
        entity.setCompletedBy(task.completedBy());
        entity.setComments(task.comments());
        entity.setCreatedAt(task.createdAt());
        return entity;
    }

    private WorkflowTask toDomain(WorkflowTaskEntity entity) {
        return new WorkflowTask(
                entity.getId(),
                new WorkflowId(entity.getWorkflowId()),
                entity.getAssignee(),
                entity.getAction(),
                entity.getStatus(),
                entity.getDueAt(),
                entity.getCompletedAt(),
                entity.getCompletedBy(),
                entity.getComments(),
                entity.getCreatedAt()
        );
    }
}
