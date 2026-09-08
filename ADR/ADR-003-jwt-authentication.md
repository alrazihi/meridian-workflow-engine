# ADR-003: JWT-Based OAuth2 Resource Server

## Status

Accepted

## Context

The API must support machine-to-machine and human authentication. We need stateless authentication that scales horizontally without sticky sessions or shared session stores.

## Decision

Implement the service as an **OAuth2 Resource Server** validating **JWTs** signed with RS256.

### Token Flow
1. Client authenticates with Authorization Server (external IdP)
2. Client receives access token (15 min) + refresh token (rotating)
3. Client sends `Authorization: Bearer <token>` on each request
4. Resource server validates signature, expiration, scope, and audience

### Authorization Model
- **Roles**: `OPERATOR`, `REVIEWER`, `ADMIN`, `SYSTEM`
- **ABAC**: Document-level access via `@PreAuthorize("hasDocumentAccess(#documentId)")`
- **Method Security**: `@PreAuthorize` on all controller methods

### Key Management
- Public keys fetched from JWKS endpoint
- Cached with TTL
- Graceful degradation on key fetch failure

## Consequences

### Positive
- Stateless, scales horizontally
- Standard protocol (OAuth2) with broad tooling support
- Fine-grained authorization via ABAC
- No session affinity required

### Negative
- Token revocation requires introspection or short TTL
- JWKS endpoint dependency
- Complex debugging of auth issues

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Session cookies | Requires sticky sessions or Redis store |
| API keys | No fine-grained scopes, no standard protocol |
| mTLS only | No human user representation, complex cert management |

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [OAuth 2.0 Threat Model](https://datatracker.ietf.org/doc/html/rfc6819)
