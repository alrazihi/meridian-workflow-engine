package com.meridian.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityConfigValidationTest {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldHaveJwtDecoderBean() {
        assertThat(jwtDecoder).isNotNull();
    }
}
