package com.meridian.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentStatusTest {

    @Test
    void shouldHaveExpectedStatuses() {
        assertThat(DocumentStatus.values()).containsExactly(
                DocumentStatus.RECEIVED,
                DocumentStatus.VALIDATING,
                DocumentStatus.ROUTED,
                DocumentStatus.PROCESSING,
                DocumentStatus.COMPLETED,
                DocumentStatus.REJECTED,
                DocumentStatus.ARCHIVED
        );
    }

    @Test
    void shouldParseStatusByName() {
        assertThat(DocumentStatus.valueOf("RECEIVED")).isEqualTo(DocumentStatus.RECEIVED);
        assertThat(DocumentStatus.valueOf("COMPLETED")).isEqualTo(DocumentStatus.COMPLETED);
    }
}
