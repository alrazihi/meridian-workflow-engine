# Architecture Diagrams

## System Context

```mermaid
graph LR
    subgraph "External"
        Browser["Browser / Angular SPA"]
        ExternalSystem["External System\n(webhook)"]
        Operator["Human Operator"]
    end

    subgraph "Meridian Workflow Engine"
        API["REST API\n(DocumentController, WorkflowController)"]
        WebhookFilter["WebhookAuthenticationFilter\n(HMAC-SHA256)"]
        Security["Security Filter Chain\n(JWT + Method Security)"]
        AppLayer["Application Layer\n(Use Cases + Orchestration)"]
        Domain["Domain Layer\n(Aggregates, Services)"]
        DB[(PostgreSQL 16\n+ Flyway)]
        Kafka[(Apache Kafka\n3.7)]
        Cache[(Redis Cache)]
        Metrics[("Prometheus\n+ Actuator")]
    end

    Browser -->|HTTPS + JWT| API
    ExternalSystem -->|JSON + HMAC| WebhookFilter
    WebhookFilter --> API
    Operator -->|Review/Approve| API

    API --> Security
    Security --> AppLayer
    AppLayer --> Domain
    AppLayer --> DB
    AppLayer --> Kafka
    AppLayer --> Cache
    AppLayer --> Metrics

    Kafka -->|Events| AppLayer
```

## Container Architecture

```mermaid
graph TB
    subgraph "meridian-workflow-service (Spring Boot 3.3)"
        direction TB
        RestAPI["REST Controllers\n@PreAuthorize on every endpoint"]
        Webhook["WebhookAuthenticationFilter\nHMAC signature validation"]
        Correlation["CorrelationIdFilter\nMDC propagation"]
        UseCases["Use Case Services\n@Transactional boundaries"]
        DomainServices["Domain Services\nDocumentValidator"]
        Repos["Repository Adapters\nJPA + Kafka"]
        Cache["CachingDocumentQueryService\n@Cacheable / @CacheEvict"]
        Metrics["WorkflowMetrics\nMicrometer counters"]
        Health["Health Indicators\nDB, Kafka, Redis"]
        Audit["AuditAspect\n@PreAuthorize logging"]
    end

    subgraph "Infrastructure"
        PostgreSQL[(PostgreSQL 16\nFlyway migrations\n@Version optimistic lock)]
        KafkaBroker[(Apache Kafka 3.7\nTopics: document.events\nworkflow.tasks\nworkflow.completed)]
        Redis[(Redis\nDocument cache\n10-min TTL)]
    end

    subgraph "External"
        JWT["JWT Provider\n(JWKS or symmetric)"]
        Prometheus[("Prometheus\n/actuator/prometheus")]
    end

    RestAPI --> UseCases
    Webhook --> RestAPI
    Correlation --> RestAPI
    UseCases --> DomainServices
    UseCases --> Repos
    Repos --> PostgreSQL
    Repos --> KafkaBroker
    Cache --> Redis
    Metrics --> Prometheus
    Health --> PostgreSQL
    Health --> KafkaBroker
    Health --> Redis
    RestAPI --> JWT
    Audit --> PostgreSQL
```

## Component Dependencies (Hexagonal)

```mermaid
graph TB
    subgraph "meridian-domain"
        Document["Document\n(aggregate)"]
        WorkflowInstance["WorkflowInstance\n(aggregate)"]
        WorkflowTask["WorkflowTask\n(entity)"]
        DocumentValidator["DocumentValidator\n(domain service)"]
        PortsIn["Inbound Ports\n(interfaces)"]
        PortsOut["Outbound Ports\n(interfaces)"]
    end

    subgraph "meridian-application"
        IngestionService["DefaultDocumentIngestionService\nimplements IngestDocumentUseCase"]
        Orchestrator["DefaultWorkflowOrchestrator\nimplements StartWorkflowUseCase\nCompleteTaskUseCase"]
        QueryService["DefaultDocumentQueryService\nimplements QueryDocumentUseCase"]
    end

    subgraph "meridian-infrastructure"
        JpaDocRepo["JpaDocumentRepository\nimplements DocumentRepository"]
        JpaTaskRepo["JpaTaskRepository\nimplements TaskRepository"]
        JpaWorkflowRepo["JpaWorkflowInstanceRepository\nimplements WorkflowInstanceRepository"]
        KafkaPub["KafkaEventPublisher\nimplements EventPublisher"]
        KafkaConsumer["DocumentEventConsumer\n@KafkaListener"]
        SecurityConfig["SecurityConfig\nOAuth2 + Method Security"]
        Controllers["REST Controllers\n@PreAuthorize"]
        MetricsComp["WorkflowMetrics\nMicrometer"]
    end

    IngestionService --> PortsOut
    Orchestrator --> PortsOut
    QueryService --> PortsOut

    PortsIn <--> IngestionService
    PortsIn <--> Orchestrator
    PortsIn <--> QueryService

    JpaDocRepo --> PortsOut
    JpaTaskRepo --> PortsOut
    JpaWorkflowRepo --> PortsOut
    KafkaPub --> PortsOut

    Controllers --> PortsIn

    IngestionService --> Document
    Orchestrator --> WorkflowInstance
    Orchestrator --> WorkflowTask
    IngestionService --> DocumentValidator
    Orchestrator --> DocumentValidator

    JpaDocRepo --> PostgreSQL[(PostgreSQL)]
    JpaTaskRepo --> PostgreSQL
    JpaWorkflowRepo --> PostgreSQL
    KafkaPub --> KafkaBroker[(Kafka)]
    KafkaConsumer --> KafkaBroker
    MetricsComp --> Prometheus[("Prometheus")]
```

## Data Flow: Document Lifecycle

```mermaid
stateDiagram-v2
    [*] --> RECEIVED: Ingest
    RECEIVED --> VALIDATING: Validation
    VALIDATING --> ROUTED: Workflow Started
    ROUTED --> PROCESSING: Task Assigned
    PROCESSING --> COMPLETED: Approved
    PROCESSING --> REJECTED: Rejected
    COMPLETED --> ARCHIVED: Archive
    REJECTED --> ARCHIVED: Archive

    note right of RECEIVED
        DocumentEntity saved
        DOCUMENT_CREATED event published
        Idempotency key checked
    end note

    note right of ROUTED
        WorkflowInstance created
        WorkflowTask created and assigned
        WORKFLOW_STARTED event published
    end note

    note right of COMPLETED
        WorkflowTask marked COMPLETED
        WorkflowState.COMPLETED
        Cache evicted
    end note
```

## Concurrency Protection

```mermaid
sequenceDiagram
    participant Client A
    participant Client B
    participant Service
    participant DB

    Client A->>Service: completeTask(taskId)
    Client B->>Service: completeTask(taskId)
    Service->>DB: SELECT ... FOR UPDATE (optimistic via @Version)
    DB-->>Service: version=0
    Service->>DB: UPDATE tasks SET status='COMPLETED', version=1
    DB-->>Service: Success
    Service-->>Client A: 200 OK

    Service->>DB: SELECT ... (version still 0)
    DB-->>Service: version=0
    Service->>DB: UPDATE tasks SET status='COMPLETED', version=1
    DB-->>Service: OptimisticLockingFailureException
    Service-->>Client B: 409 Conflict
```

## Event Processing Pipeline

```mermaid
graph LR
    subgraph "Producer Side"
        Orchestrator["DefaultWorkflowOrchestrator"]
        Publisher["KafkaEventPublisher\n@Transactional"]
        Topic1["document.events"]
        Topic2["workflow.tasks"]
        Topic3["workflow.completed"]
    end

    subgraph "Consumer Side"
        Listener["DocumentEventConsumer\n@KafkaListener\ngroup-id: meridian-workflow-service"]
        WriteRepo["DocumentRepository\nupdateStatus()"]
        ReadRepo["DocumentReadRepository\nupdateStatus()"]
        Cache["CachingDocumentQueryService\nevictDocument()"]
    end

    Orchestrator -->|publish()| Publisher
    Publisher -->|send| Topic1
    Publisher -->|send| Topic2
    Publisher -->|send| Topic3

    Topic1 -->|@KafkaListener| Listener
    Topic2 -->|@KafkaListener| Listener
    Topic3 -->|@KafkaListener| Listener

    Listener -->|1. update| WriteRepo
    Listener -->|2. update| ReadRepo
    Listener -->|3. evict| Cache

    WriteRepo --> DB[(PostgreSQL\nwrite model)]
    ReadRepo --> DB2[(PostgreSQL\nread model)]
    Cache --> Redis[(Redis)]
```
