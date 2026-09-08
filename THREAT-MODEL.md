# Threat Model

## Scope

The threat model covers the `meridian-workflow-engine` backend service and its interactions with PostgreSQL, Kafka, and Redis.

## Assets

| Asset | Sensitivity | Description |
|-------|-------------|-------------|
| Document Content | High | Binary files, PII, financial data |
| Workflow State | Medium | Business process data |
| Audit Logs | High | Immutable access records |
| JWT Signing Key | Critical | Token forgery risk |
| Database Credentials | Critical | Full data access |
| Kafka Credentials | High | Event stream poisoning |

## STRIDE Analysis

### Spoofing

| Threat | Mitigation |
|--------|------------|
| Token forgery | RS256 JWT validation, short expiration, key rotation |
| Impersonation | Scope claims validated at every endpoint |
| Webhook spoofing | HMAC signature verification on inbound webhooks |

### Tampering

| Threat | Mitigation |
|--------|------------|
| Request modification | TLS 1.3 everywhere, HSTS headers |
| Event replay | Idempotency keys, event sequence numbers |
| Audit log modification | Append-only design, separate retention policy |

### Repudiation

| Threat | Mitigation |
|--------|------------|
| Denied document access | Signed audit entries with user identity + timestamp |
| Denied workflow action | Event sourcing for all state transitions |

### Information Disclosure

| Threat | Mitigation |
|--------|------------|
| Unauthorized document access | ABAC at repository and service layers |
| PII exposure | Encryption at rest for sensitive metadata fields |
| Error message leakage | Generic error responses, detailed logging server-side only |
| Metadata enumeration | Rate limiting, pagination with cursors |

### Denial of Service

| Threat | Mitigation |
|--------|------------|
| Ingestion flood | Rate limiting, request validation, async processing |
| Kafka backlog | Consumer scaling, dead-letter queue, monitoring |
| Database exhaustion | Connection pooling, query optimization, circuit breakers |

### Elevation of Privilege

| Threat | Mitigation |
|--------|------------|
| Role escalation | Server-side role validation, no client-side trust |
| Workflow bypass | State machine enforcement at domain layer |
| Admin API abuse | Separate admin endpoints with stricter auth |

## Security Controls

### Preventive
- Input validation on all boundaries
- OWASP dependency checking in CI
- SAST scanning (SpotBugs, Checkstyle)
- Secret scanning (Gitleaks, truffleHog)
- Container image scanning (Trivy)

### Detective
- Structured audit logging for all access
- Anomaly detection on ingestion patterns
- Security metrics: failed auth rate, rate limit hits
- Alerting on Kafka consumer lag spikes

### Corrective
- Automated account lockout after failed auth
- Document quarantine on validation failure
- Circuit breaker activation on downstream failures
- Incident response runbooks

## Assumptions

- IdP (identity provider) is external and trusted
- Network perimeter is controlled (service mesh or firewall)
- Secrets management via environment variables or vault
- Monitoring infrastructure is operational
