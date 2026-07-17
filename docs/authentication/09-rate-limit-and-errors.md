# Rate Limiting and Error Handling

## Login throttling

```mermaid
flowchart TD
    A[POST login] --> B[Resolve trusted client IP]
    B --> C{Redis mode enabled?}
    C -- No --> D[Bucket4j local bucket]
    C -- Yes --> E[Redis atomic counter with TTL]
    D --> F{At most 10 per minute?}
    E --> F
    F -- No --> G[429 Too many login attempts]
    F -- Yes --> H[Continue to authentication]
```

## Error response

```json
{
  "success": false,
  "message": "Unauthorized",
  "timestamp": "2026-07-14T11:03:28.846216",
  "errors": null,
  "path": "/api/v1/auth/login"
}
```

```mermaid
flowchart LR
    V[Validation failure] --> E400[400]
    A[Bad credentials or session] --> E401[401]
    P[Role/permission/CSRF denied] --> E403[403]
    N[Resource missing] --> E404[404]
    D[Duplicate constraint] --> E409[409]
    R[Rate exceeded] --> E429[429]
    U[Unexpected failure] --> E500[500 generic message]
```

Stack traces and internal SQL/security details are never returned to clients.
