package com.meridian.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentAcl(
        String id,
        String documentId,
        String actor,
        String permission,
        Instant grantedAt
) {
    public static DocumentAcl grant(String documentId, String actor, String permission) {
        return new DocumentAcl(
                UUID.randomUUID().toString(),
                documentId,
                actor,
                permission,
                Instant.now()
        );
    }
}
