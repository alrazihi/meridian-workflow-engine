package com.meridian.infrastructure.security.config;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;

    public MethodSecurityConfig(DocumentRepository documentRepository, TaskRepository taskRepository) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setExpressionRootProvider(() -> new DocumentSecurityExpressionRoot(
                getAuthentication(), documentRepository, taskRepository));
        return handler;
    }
}
