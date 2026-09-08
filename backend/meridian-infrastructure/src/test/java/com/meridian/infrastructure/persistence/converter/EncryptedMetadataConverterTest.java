package com.meridian.infrastructure.persistence.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EncryptedMetadataConverterTest {

    @Autowired
    private EncryptedMetadataConverter converter;

    @Test
    void shouldEncryptAndDecryptMetadata() {
        String plaintext = "{\"sensitive\": \"data\"}";
        String encrypted = converter.convertToDatabaseColumn(plaintext);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
