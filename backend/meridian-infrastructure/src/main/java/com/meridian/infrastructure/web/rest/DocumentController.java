package com.meridian.infrastructure.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.infrastructure.web.dto.DocumentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private static final String WEBHOOK_SECRET = System.getenv().getOrDefault("WEBHOOK_SECRET", "changeme-webhook-secret");

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final QueryDocumentUseCase queryDocumentUseCase;
    private final ObjectMapper objectMapper;

    public DocumentController(IngestDocumentUseCase ingestDocumentUseCase, QueryDocumentUseCase queryDocumentUseCase, ObjectMapper objectMapper) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.queryDocumentUseCase = queryDocumentUseCase;
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

        Document document = ingestDocumentUseCase.ingest(file.getBytes(), type, priority, metadata, idempotencyKey);
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/webhooks/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> ingestDocumentViaWebhook(
            @RequestHeader("X-HMAC-Signature") String signature,
            @RequestBody Map<String, Object> payload
    ) {
        String typeStr = (String) payload.get("type");
        DocumentType type = DocumentType.valueOf(typeStr);
        String priority = (String) payload.getOrDefault("priority", "NORMAL");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) payload.getOrDefault("metadata", Map.of());
        String idempotencyKey = (String) payload.get("idempotencyKey");

        Document document = ingestDocumentUseCase.ingest(
                payload.get("content").toString().getBytes(),
                type,
                priority,
                metadata,
                idempotencyKey
        );
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        List<Document> documents = queryDocumentUseCase.listDocuments();
        List<DocumentResponse> responses = documents.stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasDocumentAccess(#documentId)")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String documentId) {
        Document document = queryDocumentUseCase.getDocument(new com.meridian.domain.model.valueobjects.DocumentId(documentId));
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.ok(response);
    }
}
