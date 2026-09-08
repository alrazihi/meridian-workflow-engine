package com.meridian.infrastructure.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HardcodedSecretsTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Test
    void shouldNotUseDefaultJwtSecretInProduction() {
        String defaultSecret = System.getenv().getOrDefault("JWT_SECRET_KEY", "");
        assertThat(defaultSecret)
                .as("JWT_SECRET_KEY environment variable should be set in production")
                .isNotEqualTo("c2VjcmV0X2tleV9mb3JfZGV2X2xpbmtlZF9pbl9sZWZ0X2RlbW9fbmV0d29yaw==");
    }
}
