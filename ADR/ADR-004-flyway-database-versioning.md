# ADR-004: Flyway for Database Versioning

## Status

Accepted

## Context

Database schema changes must be versioned, repeatable, and auditable. The team needs a mechanism that works across local development, CI, and production without manual steps or schema drift.

## Decision

Use **Flyway** for versioned database migrations.

### Principles
- Every schema change is a versioned SQL script
- Scripts named `V{version}__{description}.sql`
- No manual schema changes in production
- Migrations run automatically on application startup
- Rollback scripts for emergency use only

### Location
`backend/meridian-infrastructure/src/main/resources/db/migration/`

## Consequences

### Positive
- Deterministic schema evolution
- Works across all environments
- SQL-based, no DSL to learn
- Strong community and Spring Boot integration

### Negative
- Requires discipline to never skip a version
- Complex refactorings may need manual data migrations
- Limited support for branching strategies

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|-----------------|
| Liquibase | XML/YAML/JSON DSL adds complexity |
| JPA auto-DDL | Non-deterministic across environments |
| Manual SQL scripts | No version tracking, error-prone |

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
