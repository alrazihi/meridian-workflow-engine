# Test Strategy

## Test Pyramid

```
        /\
       /  \     E2E (10%)
      /____\    - Full stack with Testcontainers
     /      \
    /________\  Integration (30%)
   /          \ - Repository + Kafka + REST
  /____________\ Unit (60%)
                 - Domain services, use cases, validators
```

## Unit Tests

**Scope:** Domain services, use cases, validators, value objects

**Tools:** JUnit 5, Mockito, AssertJ

**Coverage Target:** 90%+ line coverage, 100% on domain module

## Integration Tests

**Scope:** Repository layer, Kafka consumers/producers, REST API

**Tools:** JUnit 5, Spring Boot Test, Testcontainers, WireMock

### Repository Tests
```java
@Testcontainers
class DocumentRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Test
    void shouldPersistAndRetrieveDocument() {
        Document document = Document.builder()
            .type(DocumentType.INVOICE)
            .metadata(Map.of("vendorId", "VEND-001"))
            .build();
        documentRepository.save(document);
        Optional<Document> found = documentRepository.findById(document.getId());
        assertThat(found).isPresent();
    }
}
```

### Kafka Tests
```java
@Testcontainers
class DocumentEventPublisherIntegrationTest {
    @Container
    static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.5.0");
    
    @Test
    void shouldPublishEventWhenDocumentValidated() {
        // Verify event appears on topic
    }
}
```

## Contract Tests

- **OpenAPI contract testing:** Spring Cloud Contract or Dredd
- **Kafka contract testing:** Spring Cloud Contract for message contracts
- **Webhook contracts:** Consumer-driven contracts for outbound callbacks

## Performance Tests

- **JMH benchmarks** for hot paths: validation, routing, serialization
- **Gatling** load tests for ingestion endpoint (target: 1000 req/s)
- **Database query profiling** via P6spy

## Quality Gates

| Metric | Target |
|--------|--------|
| Unit test coverage | >= 90% |
| Integration test coverage | >= 70% |
| Mutation testing score | >= 80% |
| Build time | < 10 minutes |
| Test execution time | < 5 minutes (unit), < 15 minutes (integration) |
