# Deployment Guide

## Local Development

```bash
docker compose up --build
```

Services:
- PostgreSQL: localhost:5432
- Kafka: localhost:9092
- Redis: localhost:6379
- Backend: localhost:8080
- Frontend: localhost:4200
- Prometheus: localhost:9090
- Grafana: localhost:3000

## Production Considerations

- Use managed PostgreSQL (RDS, Cloud SQL)
- Use managed Kafka (Confluent, MSK)
- Secrets via environment variables or vault
- Health checks and readiness probes
- Resource limits and requests
- Persistent volumes for Kafka
- Network policies for service isolation
