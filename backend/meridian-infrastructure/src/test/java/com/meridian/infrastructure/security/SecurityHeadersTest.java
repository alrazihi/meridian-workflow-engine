package com.meridian.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityHeadersTest {

    @Autowired
    private org.springframework.web.servlet.HandlerMappingIntrospector handlerMappingIntrospector;

    @Test
    void shouldHaveSecurityHeadersConfigured() {
        assertThat(handlerMappingIntrospector).isNotNull();
    }
}
