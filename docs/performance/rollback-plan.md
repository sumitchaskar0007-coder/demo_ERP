# Rollback plan

## Application

1. Keep the previous immutable ECR digest and ECS task-definition revision.
2. ECS deployment circuit breaker performs automatic rollback on failed health checks.
3. For a functional regression, update the staging service to the previous task revision and wait for target health.
4. Restore the matching frontend version and invalidate CloudFront.

## Database

- Flyway V22 only adds version columns and indexes; do not drop them during an emergency rollback.
- Roll application code back while leaving additive schema intact.
- Before any RDS class change, take a manual snapshot and record the original instance class/parameter group.
- Reverting RDS size requires a maintenance event and separate approval.

## WAF and scaling

- Retain the pre-change Terraform plan/state backup.
- Revert the WAF rate threshold through Terraform if abuse increases.
- Disable scheduled actions and restore min 2/max 6 if capacity controls behave unexpectedly.

## Queues

- Stop producers first, then drain workers.
- Preserve DLQ messages for diagnosis; never purge as a rollback step.
- Reprocess only idempotent messages after the cause is fixed.

## SSE/cache

- Frontend fallback polling continues if SSE is unavailable.
- Disable authorization caching with `AUTHORIZATION_CACHE_ENABLED=false`; PostgreSQL remains the safe fallback.

## Decision authority

Staging rollback may proceed under the approved staging change window. Production rollback must follow the production incident/change owner unless immediate automated ECS rollback is already configured.
