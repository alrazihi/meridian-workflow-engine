package com.meridian.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WorkflowMetrics {

    private final Counter documentIngestedCounter;
    private final Counter workflowStartedCounter;
    private final Counter taskCompletedCounter;
    private final Counter eventPublishFailureCounter;
    private final Counter optimisticLockFailureCounter;

    public WorkflowMetrics(MeterRegistry meterRegistry) {
        this.documentIngestedCounter = Counter.builder("documents.ingested")
                .description("Total number of documents ingested")
                .register(meterRegistry);
        this.workflowStartedCounter = Counter.builder("workflows.started")
                .description("Total number of workflows started")
                .register(meterRegistry);
        this.taskCompletedCounter = Counter.builder("tasks.completed")
                .description("Total number of tasks completed")
                .register(meterRegistry);
        this.eventPublishFailureCounter = Counter.builder("events.publish.failures")
                .description("Total number of event publish failures")
                .register(meterRegistry);
        this.optimisticLockFailureCounter = Counter.builder("optimistic.lock.failures")
                .description("Total number of optimistic lock failures")
                .register(meterRegistry);
    }

    public void incrementDocumentIngested() {
        documentIngestedCounter.increment();
    }

    public void incrementWorkflowStarted() {
        workflowStartedCounter.increment();
    }

    public void incrementTaskCompleted() {
        taskCompletedCounter.increment();
    }

    public void incrementEventPublishFailure() {
        eventPublishFailureCounter.increment();
    }

    public void incrementOptimisticLockFailure() {
        optimisticLockFailureCounter.increment();
    }
}