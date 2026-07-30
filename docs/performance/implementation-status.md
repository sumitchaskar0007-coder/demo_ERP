# Hardening implementation status

Date: 2026-07-28

This branch is not evidence that the system supports 5,000 simultaneous
users. Capacity remains blocked on an approved isolated staging deployment
and two successful load-test runs.

| Requirement | Status | Implementation/evidence | AWS impact | Remaining risk / next action |
|---|---|---|---|---|
| Verified repository and AWS inventory | Completed | `5000-user-readiness-audit.md` | Read-only inspection only | Replace root AWS use with an assumed role |
| Distributed rate limiting | Completed locally | User-ID API buckets, account+IP login bucket, trusted proxy resolution, configurable limits and 429 tests | Proposed WAF threshold only | Validate shared-campus traffic in staging |
| Notice delivery | Completed locally | Redis-backed SSE, heartbeat/reconnect, unread-count-only 90-second visible-tab fallback | Uses existing Valkey | Run multi-task reconnect test |
| Authorization snapshots | Partially completed | Minimal Redis snapshot, DB fallback, metrics, short TTL, invalidation for central user/status/password/session paths | Uses existing Valkey | Add invalidation to every remaining direct role mutation and test Redis outage under load |
| Database optimization | Partially completed | Notice pagination fix, hot indexes, version columns, 5-second query timeout and connection-budget guard | Additive Flyway migration | Replace remaining academic/attendance Java-side `findAll()` filters and validate indexes with synthetic data |
| Direct S3 transfer | Blocked | Existing bucket is private/encrypted and keys are validated, but web requests still buffer files | None | Implement presigned upload metadata, completion verification and malware quarantine before enabling |
| Asynchronous email | Partially completed | Existing transactional DB queue already retries and tracks status; Terraform adds SQS/DLQ/alarms | Would create SQS queues | Wire producers/workers to SQS and obtain SES production access approval |
| Asynchronous reports | Partially completed | Terraform adds report queue/DLQ/alarms | Would create SQS queues and objects | Implement job entity, worker, status API and private presigned result |
| Concurrent update protection | Partially completed | `@Version`, Flyway columns and 409 response on selected mutable records | Database migration | Add a real two-transaction conflict integration test and extend only to proven mutable aggregates |
| ECS scaling | Completed locally | 2–12 targets, CPU/memory/request scaling, optional 8-task schedule, cooldowns, DB connection precondition | Would modify ECS autoscaling | Review plan/cost, then approve staging apply |
| RDS plan | Completed as analysis | Cost/rollback/runbook describes r7g and Proxy evaluation | None | Benchmark r7g.large and Proxy in staging; no resize approved |
| Monitoring | Partially completed | ALB/ECS/RDS/Redis/SQS/SES/429/Hikari alarms and capacity dashboard | Would create CloudWatch/SNS resources | Provide alert recipient; add JVM/Hikari gauges through the telemetry pipeline |
| Load testing | Completed locally, not executed | Guarded k6 ladder and runbook | None until approved staging run | Supply synthetic users and approve isolated staging test |
| CI/CD | Completed locally | Java, frontend, browser, Terraform, image, vulnerability and secret checks; OIDC deploy gate | No deployment from `sumit` | GitHub billing must remain active; request approval before push |
| Frontend startup/session behavior | Completed locally | Route-level lazy loading, cross-tab refresh lock and current-device logout | None | Browser-test multiple devices/tabs in staging |

## Local validation record

- Backend Maven suite: 171 tests passed.
- Frontend unit suite: 17 tests passed.
- Frontend production build and lint passed (zero errors; 21 existing hook/
  fast-refresh warnings).
- Terraform recursive formatting and validation passed.
- No AWS write, deployment, load test, database migration or Terraform apply
  was performed.
