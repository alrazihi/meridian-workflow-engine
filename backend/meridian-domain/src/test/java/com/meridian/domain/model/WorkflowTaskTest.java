package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.WorkflowId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowTaskTest {

    @Test
    void shouldCreateTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        );

        assertThat(task.id()).isNotNull();
        assertThat(task.assignee()).isEqualTo("group:reviewers");
        assertThat(task.action()).isEqualTo("REVIEW");
        assertThat(task.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.completedAt()).isNull();
        assertThat(task.completedBy()).isNull();
    }

    @Test
    void shouldRejectTaskWithNullWorkflowId() {
        assertThatThrownBy(() -> new WorkflowTask(
                "task-1", null, "group", "REVIEW", TaskStatus.PENDING,
                null, null, null, null, Instant.now(), 0L
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectTaskWithNullAssignee() {
        assertThatThrownBy(() -> new WorkflowTask(
                "task-1", WorkflowId.generate(), null, "REVIEW", TaskStatus.PENDING,
                null, null, null, null, Instant.now(), 0L
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectTaskWithNullAction() {
        assertThatThrownBy(() -> new WorkflowTask(
                "task-1", WorkflowId.generate(), "group", null, TaskStatus.PENDING,
                null, null, null, null, Instant.now(), 0L
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectTaskWithNullStatus() {
        assertThatThrownBy(() -> new WorkflowTask(
                "task-1", WorkflowId.generate(), "group", "REVIEW", null,
                null, null, null, null, Instant.now(), 0L
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldAssignTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        );

        WorkflowTask assigned = task.assign();
        assertThat(assigned.status()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(assigned.id()).isEqualTo(task.id());
        assertThat(assigned.assignee()).isEqualTo(task.assignee());
    }

    @Test
    void shouldCompleteTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        ).assign();

        WorkflowTask completed = task.complete("user-123", "Looks good");
        assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.completedBy()).isEqualTo("user-123");
        assertThat(completed.completedAt()).isNotNull();
        assertThat(completed.comments()).isEqualTo("Looks good");
    }

    @Test
    void shouldRejectCompleteOnAlreadyCompletedTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        ).assign().complete("user-123", "Looks good");

        assertThatThrownBy(() -> task.complete("user-456", "Second completion"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void shouldPreserveWorkflowIdThroughStateChanges() {
        WorkflowId workflowId = WorkflowId.generate();
        WorkflowTask task = WorkflowTask.create(workflowId, "group", "REVIEW");
        WorkflowTask assigned = task.assign();
        WorkflowTask completed = assigned.complete("user", "ok");

        assertThat(completed.workflowId()).isEqualTo(workflowId);
    }
}
