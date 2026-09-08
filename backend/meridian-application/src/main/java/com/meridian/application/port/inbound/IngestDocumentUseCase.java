package com.meridian.application.port.inbound;

import com.meridian.domain.model.Document;
import com.meridian.domain.model.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IngestDocumentUseCase {
    Document ingest(MultipartFile file, DocumentType type, String priority, Map<String, Object> metadata, String idempotencyKey);
}
