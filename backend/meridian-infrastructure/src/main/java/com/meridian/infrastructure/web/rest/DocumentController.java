package com.meridian.infrastructure.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.application.port.inbound.IngestDocumentUseCase;
import com.meridian.application.port.inbound.QueryDocumentUseCase;
import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import com.meridian.infrastructure.web.dto.DocumentResponse;
import com.meridian.infrastructure.web.dto.WebhookDocumentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Documents", description = "Document ingestion and query operations")
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final IngestDocumentUseCase ingestDocumentUseCase;
    private final QueryDocumentUseCase queryDocumentUseCase;
    private final ObjectMapper objectMapper;

    public DocumentController(IngestDocumentUseCase ingestDocumentUseCase, QueryDocumentUseCase queryDocumentUseCase, ObjectMapper objectMapper) {
        this.ingestDocumentUseCase = ingestDocumentUseCase;
        this.queryDocumentUseCase = queryDocumentUseCase;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Ingest a new document", description = "Upload a document for processing. Requires OPERATOR role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate idempotency key or invalid state transition", content = @Content)
    })
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
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid metadata JSON: " + e.getMessage(), e);
            }
        }

        Document document = ingestDocumentUseCase.ingest(file.getBytes(), type, priority, metadata, idempotencyKey);
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Ingest document via webhook", description = "External webhook endpoint for document ingestion. Authenticated via HMAC signature.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid HMAC signature", content = @Content)
    })
    @PostMapping(value = "/webhooks/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> ingestDocumentViaWebhook(
            @RequestHeader("X-HMAC-Signature") String signature,
            @Valid @RequestBody WebhookDocumentRequest request
    ) {
        DocumentType type = DocumentType.valueOf(request.type());
        String priority = request.priority() != null ? request.priority() : "NORMAL";
        Map<String, Object> metadata = request.metadata() != null ? request.metadata() : Map.of();
        String idempotencyKey = request.idempotencyKey();

        Document document = ingestDocumentUseCase.ingest(
                request.content().getBytes(),
                type,
                priority,
                metadata,
                idempotencyKey
        );
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all documents", description = "Returns a paginated list of documents. Requires OPERATOR or ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of documents", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @GetMapping("/documents")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 0 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters: page must be >= 0, size must be between 1 and 100");
        }

        List<Document> documents = queryDocumentUseCase.listDocuments();
        int start = page * size;
        int end = Math.min(start + size, documents.size());
        List<DocumentResponse> responses = documents.subList(start, end).stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get document by ID", description = "Returns a single document. Requires document access permission.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - no access to document", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document not found", content = @Content)
    })
    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasDocumentAccess(#documentId)")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String documentId) {
        Document document = queryDocumentUseCase.getDocument(new com.meridian.domain.model.valueobjects.DocumentId(documentId));
        DocumentResponse response = DocumentResponse.from(document);
        return ResponseEntity.ok(response);
    }
}
