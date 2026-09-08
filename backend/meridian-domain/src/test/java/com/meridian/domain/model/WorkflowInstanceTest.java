package com.meridian.domain.model;

import com.meridian.domain.model.valueobjects.WorkflowId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowInstanceTest {

    @Test
    void shouldStartWorkflow() {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"),
                "corr-456"
        );

        assertThat(instance.id()).isNotNull();
        assertThat(instance.state()).isEqualTo("STARTED");
        assertThat(instance.documentId().value()).isEqualTo("doc-123");
        assertThat(instance.correlationId()).isEqualTo("corr-456");
    }

    @Test
    void shouldTransitionState() {
        WorkflowInstance instance = WorkflowInstance.start(
                new com.meridian.domain.model.valueobjects.DocumentId("doc-123"),
                "corr-456"
        );

        WorkflowInstance updated = instance.withState("ROUTED");
        assertThat(updated.state()).isEqualTo("ROUTED");
        assertThat(updated.version()).isEqualTo(1L);
    }

    @Test
    void shouldRejectNullDocumentId() {
        assertThatThrownBy(() -> WorkflowInstance.start(null, "corr-456"))
                .isInstanceOf(NullPointerException.class);
    }
}
