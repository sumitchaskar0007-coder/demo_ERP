# Production readiness

Run the backend with `SPRING_PROFILES_ACTIVE=production`. Production mode validates the schema, executes Flyway migrations, requires shared Redis rate limiting, enables graceful shutdown, and exposes liveness/readiness probes.

## Multi-instance requirements

- Run at least two backend instances behind a load balancer.
- Mount the same persistent volume at `SHARED_STORAGE_DIR` on every backend instance.
- Use one managed PostgreSQL database with automated backups and point-in-time recovery.
- Use one shared Redis service. Do not use instance-local rate limiting in production.
- Configure the load balancer to use `/actuator/health/readiness` and `/actuator/health/liveness`.
- Keep backend instances stateless; authentication state is stored in signed cookies/database tokens.

## Starting capacity

For 20 colleges and 10,000 students, begin with two application instances, each with 2 vCPU and 4 GB RAM, and a PostgreSQL connection pool of 20 per instance. Confirm the final size with a load test that reproduces login, dashboard, attendance, fee, report, and upload traffic.

The total configured connection pools across every instance must remain safely below the PostgreSQL connection limit. For example, four instances with `DB_POOL_MAX_SIZE=20` can request up to 80 application connections.

## Deployment checks

1. Back up PostgreSQL.
2. Apply the new container/image using a rolling deployment.
3. Wait for readiness to become `UP` before receiving traffic.
4. Verify `/actuator/health/readiness` includes database, Redis, and storage.
5. Monitor HTTP latency, error rate, JVM heap, database connections, slow queries, disk capacity, and Redis availability.
