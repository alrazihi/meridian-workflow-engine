package com.meridian.infrastructure.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        try {
            var producer = kafkaTemplate.getProducerFactory().createProducer();
            producer.initTransactions();
            Health.Builder builder = Health.up()
                    .withDetail("kafka", "available");
            return builder.build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
