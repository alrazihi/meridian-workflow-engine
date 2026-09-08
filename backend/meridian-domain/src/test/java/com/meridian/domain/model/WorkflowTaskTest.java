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
        assertThat(task.status()).isEqualTo("PENDING");
    }

    @Test
    void shouldAssignTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        );

        WorkflowTask assigned = task.assign();
        assertThat(assigned.status()).isEqualTo("ASSIGNED");
    }

    @Test
    void shouldCompleteTask() {
        WorkflowTask task = WorkflowTask.create(
                WorkflowId.generate(),
                "group:reviewers",
                "REVIEW"
        ).assign();

        WorkflowTask completed = task.complete("user-123", "Looks good");
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.completedBy()).isEqualTo("user-123");
        assertThat(completed.completedAt()).isNotNull();
    }
}
