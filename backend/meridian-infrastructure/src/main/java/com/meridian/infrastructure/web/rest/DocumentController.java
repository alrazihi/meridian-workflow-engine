package com.meridian.infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.QueryWorkflowStatusUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.infrastructure.web.dto.DocumentResponse;
import com.meridian.infrastructure.web.dto.WorkflowStatusResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final QueryWorkflowStatusUseCase queryWorkflowStatusUseCase;
    private final ObjectMapper objectMapper;

    public DocumentController(IngestDocumentUseCase ingestDocumentUseCase, QueryWorkflowStatusUseCase queryWorkflowStatusUseCase, ObjectMapper objectMapper) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.queryWorkflowStatusUseCase = queryWorkflowStatusUseCase;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<DocumentResponse> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type,
            @RequestParam(value = "priority", required = false, defaultValue = "NORMAL") String priority,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey
    ) {
        Map<String, Object> metadata = Map.of();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                metadata = objectMapper.readValue(metadataJson, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid metadata JSON", e);
            }
        }

        Document document = ingestDocumentUseCase.ingest(file, type, priority, metadata, idempotencyKey);
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('REVIEWER') or hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String documentId) {
        DocumentResponse response = DocumentResponse.from(null);
        return ResponseEntity.ok(response);
    }
}
