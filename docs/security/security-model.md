# Security Model

## Authentication Flow

```
┌──────┐     Authorization Code + PKCE      ┌──────────┐
│ Client│────────────────────────────────────►│   IdP    │
│      │◄────────────────────────────────────│          │
│      │     Access Token (JWT)             └──────────┘
└──┬───┘
   │
   │ Authorization: Bearer <token>
   ▼
┌─────────────────────────────────────────────────────┐
│              meridian-workflow-service               │
│  ┌─────────────┐                                    │
│  │ JwtDecoder  │──validates signature, exp via HS256│
│  └──────┬──────┘                                    │
│         │                                           │
│  ┌──────▼─────────────────────────────┐             │
│  │      Method Security              │             │
│  │  @PreAuthorize hasRole('REVIEWER')│             │
│  │  @PreAuthorize hasRole('ADMIN')   │             │
│  └───────────────────────────────────┘             │
└─────────────────────────────────────────────────────┘
```

## Roles & Permissions

| Role | Capabilities |
|------|-------------|
| `OPERATOR` | Ingest documents, query documents |
| `REVIEWER` | View assigned tasks, approve/reject |
| `ADMIN` | View all documents, manage routing rules |
| `SYSTEM` | Internal service accounts only |

## Authorization Rules

### Document Access
```java
@PreAuthorize("hasRole('OPERATOR') or hasRole('REVIEWER') or hasRole('ADMIN')")
DocumentResponse getDocument(String documentId);
```

### Workflow Access
```java
@PreAuthorize("hasRole('REVIEWER')")
WorkflowStatusResponse completeTask(...);
```

## Data Protection

### Encryption at Rest
- ~~Document metadata JSONB fields containing PII encrypted via JPA `AttributeConverter`~~ — **PLANNED**
- ~~Algorithm: AES-256-GCM~~ — **PLANNED**
- Key management: environment variable or Spring Vault — **PLANNED**

### Encryption in Transit
- TLS 1.3 required for all external connections — **PLANNED**
- mTLS optional for internal service mesh — **PLANNED**

### Secrets Management
- No secrets in code or configuration files
- Environment variables for local development
- HashiCorp Vault or AWS Secrets Manager for production — **PLANNED**

## Audit Logging

All security-relevant events logged with immutable entries — **PLANNED**:

| Event | Fields |
|-------|--------|
| Document access | userId, documentId, action, timestamp, ipAddress |
| Workflow action | userId, workflowId, taskId, decision, timestamp |
| Auth failure | username, reason, ipAddress, timestamp |
| Permission change | adminId, targetUser, roleChange, timestamp |
