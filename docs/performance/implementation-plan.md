# Implementation plan

## P0: protect the login wave

1. Deploy the user/account-aware rate limiter to isolated staging.
2. Apply the reviewed WAF change only after confirming campus egress patterns.
3. Validate legitimate shared-NAT login traffic and distributed brute-force rejection.
4. Deploy Redis authorization snapshots and verify invalidation on password, role and status changes.
5. Deploy SSE notice count events; confirm reconnects across ECS tasks and verify fallback polling.

## P1: remove database and request-thread bottlenecks

1. Replace unbounded academic, timetable and attendance `findAll()` operations with scoped pageable repository queries.
2. Add repository projections for dashboards and reports.
3. Define a total RDS connection budget; begin with 10–15 Hikari connections per task, not an independent maximum chosen per service.
4. Evaluate RDS Proxy in staging and record connection pinning before production adoption.
5. Compare `db.r7g.large` and `db.r7g.xlarge` using the same test dataset.
6. Move S3 transfers to tenant-bound presigned requests with persisted upload state and quarantine scanning.
7. Complete SQS-backed email and report workers; run them independently from web tasks.

## P2: operational readiness

1. Confirm an SNS alert recipient.
2. Add OpenTelemetry/CloudWatch metrics for Hikari, JVM, job queues and application outcomes.
3. Run migration validation and concurrency tests.
4. Execute the staged k6 ladder and stop on safety-alarm activation.
5. Capture p50/p95/p99, throughput, errors, pool waits, DB load, Redis evictions and ECS scaling events.

## Completion rules

The system may be called 5,000-user ready only when every acceptance threshold in the load-test runbook passes twice on the release candidate. Production changes require a separate approval after staging evidence.
