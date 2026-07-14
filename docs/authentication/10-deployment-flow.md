# Production Deployment Flow

```mermaid
flowchart LR
    B[Browser] -->|HTTPS| W[WAF / load balancer]
    W -->|trusted forwarded headers| RP[Reverse proxy]
    RP -->|private network| API1[Spring Boot instance 1]
    RP -->|private network| API2[Spring Boot instance 2]
    API1 --> PG[(PostgreSQL primary)]
    API2 --> PG
    API1 --> REDIS[(Private Redis)]
    API2 --> REDIS
    SM[Secret manager] --> API1
    SM --> API2
    LOG[Central monitoring] <-->|sanitized telemetry| API1
    LOG <-->|sanitized telemetry| API2
```

## Deployment checklist

```mermaid
flowchart TD
    A[Deploy] --> B[Set AUTH_COOKIE_SECURE=true]
    B --> C[Use strong managed JWT_SECRET]
    C --> D[Exact credentialed CORS origins]
    D --> E[Enable Redis rate limiting]
    E --> F[Use JPA validate + schema migrations]
    F --> G[Restrict database and Redis networks]
    G --> H[Trust proxy headers only from proxy]
    H --> I[Enable HTTPS and HSTS at edge]
    I --> J[Monitor 401, rotation reuse, and 429 rates]
    J --> K[Rotate secrets and rehearse session revocation]
```

Required production values include `AUTH_COOKIE_SECURE=true`, `RATE_LIMIT_REDIS_ENABLED=true`, exact `CORS_ALLOWED_ORIGINS`, `JPA_DDL_AUTO=validate`, and a secret-manager supplied `JWT_SECRET`.
