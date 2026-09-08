package com.meridian.infrastructure.web.rest;

import com.meridian.application.port.outbound.AuditService;
import com.meridian.domain.model.AuditLog;
import com.meridian.infrastructure.web.dto.AuditLogResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Audit", description = "Audit log queries (ADMIN only)")
@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private static final int MAX_AUDIT_RESULTS = 1000;

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "Query audit logs", description = "Returns audit log entries. Requires ADMIN role. Maximum 1000 results per request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit log entries", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> queryAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to");
        }

        List<AuditLog> logs = auditService.query(actor, resourceType, resourceId, from, to);
        List<AuditLogResponse> responses = logs.stream()
                .limit(MAX_AUDIT_RESULTS)
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
