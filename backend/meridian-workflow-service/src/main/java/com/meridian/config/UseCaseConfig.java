package com.meridian.config;

import com.meridian.application.service.DefaultDocumentIngestionService;
import com.meridian.application.service.DefaultDocumentQueryService;
import com.meridian.application.service.DefaultWorkflowOrchestrator;
import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.application.port.inbound.StartWorkflowUseCase;
import com.meridian.application.port.inbound.CompleteTaskUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public IngestDocumentUseCase ingestDocumentUseCase(
            DefaultDocumentIngestionService service) {
        return service;
    }

    @Bean
    public QueryDocumentUseCase queryDocumentUseCase(
            DefaultDocumentQueryService service) {
        return service;
    }

    @Bean
    public StartWorkflowUseCase startWorkflowUseCase(
            DefaultWorkflowOrchestrator orchestrator) {
        return orchestrator;
    }

    @Bean
    public CompleteTaskUseCase completeTaskUseCase(
            DefaultWorkflowOrchestrator orchestrator) {
        return orchestrator;
    }

    @Bean
    public QueryWorkflowStatusUseCase queryWorkflowStatusUseCase(
            DefaultWorkflowOrchestrator orchestrator) {
        return orchestrator;
    }
}
