package com.meridian.application.service;

import com.meridian.application.port.outbound.DocumentRepository;
import com.meridian.application.port.outbound.EventPublisher;
import com.meridian.application.port.outbound.NotificationService;
import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentEvent;
import com.meridian.domain.model.DocumentStatus;
import com.meridian.domain.model.DocumentType;
import com.meridian.domain.service.DocumentValidator;
import com.meridian.infrastructure.observability.WorkflowMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultDocumentIngestionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Test
    void shouldIngestValidDocument() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        Document saved = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        Document result = service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of("vendorId", "VEND-001"), null);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(result.contentHash()).isEqualTo("hash123");
    }

    @Test
    void shouldRejectDuplicateIdempotencyKey() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        when(documentRepository.existsByIdempotencyKey("key-123")).thenReturn(true);

        assertThatThrownBy(() -> service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of(), "key-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate idempotency key");

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectInvalidDocumentByValidator() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        when(documentRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document metadata is required");

        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldPublishEventAfterSuccessfulIngest() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        Document saved = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of("vendorId", "VEND-001"), null);

        ArgumentCaptor<DocumentEvent> eventCaptor = ArgumentCaptor.forClass(DocumentEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("DOCUMENT_CREATED");
        assertThat(eventCaptor.getValue().documentId()).isEqualTo(saved.id());
    }

    @Test
    void shouldComputeSha256Hash() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        Document saved = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        Document result = service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of("vendorId", "VEND-001"), null);

        assertThat(result.contentHash()).isNotNull();
        assertThat(result.contentHash()).hasSize(64);
    }

    @Test
    void shouldAllowNullIdempotencyKey() {
        DocumentValidator validator = new DocumentValidator();
        WorkflowMetrics workflowMetrics = new WorkflowMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        DefaultDocumentIngestionService service = new DefaultDocumentIngestionService(
                documentRepository, eventPublisher, validator, notificationService, workflowMetrics, auditService);

        Document saved = Document.create("hash123", DocumentType.INVOICE, Map.of("vendorId", "VEND-001"), null, "test-tenant");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);
        when(documentRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        Document result = service.ingest("test".getBytes(), DocumentType.INVOICE, "NORMAL", Map.of("vendorId", "VEND-001"), null);
    }
}
