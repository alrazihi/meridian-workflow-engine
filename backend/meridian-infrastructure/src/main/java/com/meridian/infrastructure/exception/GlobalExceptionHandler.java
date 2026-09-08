package com.meridian.infrastructure.exception;

import com.meridian.infrastructure.observability.WorkflowMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final WorkflowMetrics workflowMetrics;

    public GlobalExceptionHandler(WorkflowMetrics workflowMetrics) {
        this.workflowMetrics = workflowMetrics;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Map<String, Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
        workflowMetrics.incrementOptimisticLockFailure();
        log.warn("Optimistic lock failure: {}", ex.getMessage());
        return errorResponse(HttpStatus.CONFLICT, "Conflict", "Resource was modified by another transaction");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Map<String, Object> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return errorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntime(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private Map<String, Object> errorResponse(HttpStatus status, String title, String detail) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", status.value());
        error.put("error", title);
        error.put("message", detail);
        error.put("timestamp", Instant.now().toString());
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            error.put("correlationId", correlationId);
        }
        return error;
    }
}