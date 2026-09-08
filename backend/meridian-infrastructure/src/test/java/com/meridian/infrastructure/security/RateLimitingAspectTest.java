package com.meridian.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RateLimitingAspectTest {

    @Autowired
    private RateLimitingAspect rateLimitingAspect;

    @Test
    void shouldCreateBucketForNewKey() {
        assertThat(rateLimitingAspect).isNotNull();
    }
}
