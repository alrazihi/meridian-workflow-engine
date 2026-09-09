package com.meridian.infrastructure.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            redisTemplate.opsForValue().get("health-check");
            Health.Builder builder = Health.up()
                    .withDetail("redis", "available");
            return builder.build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
