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
│  │ JwtAuthConv │──validates signature, exp, scope   │
│  └──────┬──────┘                                    │
│         │                                           │
│  ┌──────▼─────────────────────────────┐             │
│  │      Method Security              │             │
│  │  @PreAuthorize hasRole('REVIEWER')│             │
│  │  @PreAuthorize hasDocumentAccess()│             │
│  └───────────────────────────────────┘             │
└─────────────────────────────────────────────────────┘
```

## Roles & Permissions

| Role | Capabilities |
|------|-------------|
| `OPERATOR` | Ingest documents, query own documents |
| `REVIEWER` | View assigned tasks, approve/reject |
| `ADMIN` | Manage routing rules, view all documents |
| `SYSTEM` | Internal service accounts only |

## Authorization Rules

### Document Access
```java
@PreAuthorize("hasDocumentAccess(#documentId)")
DocumentResponse getDocument(String documentId);
```

Implementation evaluates:
1. User has `ADMIN` role → allow
2. User is assignee on any active task for document → allow
3. User belongs to group with document-level ACL entry → allow
4. Otherwise → deny

## Data Protection

### Encryption at Rest
- Document metadata JSONB fields containing PII encrypted via JPA `AttributeConverter`
- Algorithm: AES-256-GCM
- Key management: environment variable or Spring Vault

### Encryption in Transit
- TLS 1.3 required for all external connections
- mTLS optional for internal service mesh

### Secrets Management
- No secrets in code or configuration files
- Environment variables for local development
- HashiCorp Vault or AWS Secrets Manager for production

## Audit Logging

All security-relevant events logged with immutable entries:

| Event | Fields |
|-------|--------|
| Document access | userId, documentId, action, timestamp, ipAddress |
| Workflow action | userId, workflowId, taskId, decision, timestamp |
| Auth failure | username, reason, ipAddress, timestamp |
| Permission change | adminId, targetUser, roleChange, timestamp |
