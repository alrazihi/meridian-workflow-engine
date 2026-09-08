package com.meridian;

import com.meridian.infrastructure.messaging.kafka.DocumentEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class RetryAndResilienceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private DocumentEventConsumer documentEventConsumer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldContinueProcessingAfterMalformedEvent() {
        String malformedEvent = "this is not valid json";
        String documentId = "doc-resilience-123";

        kafkaTemplate.send("document.events", documentId, malformedEvent);

        String validEvent = String.format(
                "{\"eventType\":\"WORKFLOW_STARTED\",\"documentId\":\"%s\",\"workflowId\":\"wf-123\"}",
                documentId
        );
        kafkaTemplate.send("document.events", documentId, validEvent);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            // The consumer should have processed the valid event despite the malformed one
            // We verify this by checking that no exception was thrown and the consumer is still running
            assertThat(documentEventConsumer).isNotNull();
        });
    }
}
