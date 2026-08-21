# Production hardening implementation plan

Status date: 2026-07-29

This plan separates safe local work from staging and production changes. It does not authorize an AWS write, deployment, database migration or load test.

## Non-negotiable gates

1. Investigate/revoke the root-console deletion session and replace the expired default root CLI login with a controlled IAM Identity Center or assumed-role session. Repeat the read-only inventory before trusting any 2026-07-28 resource ID.
2. Keep ECS/Fargate behind an ALB as the authoritative production backend architecture in `ap-south-1`. `infra/terraform-ec2` is a legacy review-only, single-instance, non-HA path and is not an acceptable 5,000-user target.
3. Obtain the externally owned production network contract: VPC ID plus at least two approved public, backend-private and, if separate, cache-private subnets. The latest inventory found no production VPC.
4. Obtain the external production RDS contract:
   - endpoint;
   - port;
   - database name;
   - production RDS security-group ID;
   - runtime database secret ARN; and
   - migration database secret ARN.
5. Apply the zero-task ECS infrastructure phase only after approval, then provide Terraform output `backend_security_group_id` to the RDS owner so PostgreSQL 5432 can be allowed only from that group. This phase still creates cost-bearing application resources.
6. Keep production Terraform from creating a VPC, RDS instance, RDS subnet group, RDS KMS key or RDS security group. Keep the existing staging-managed behavior unchanged.
7. Resolve Terraform state ownership for `collegeerp.example` / hosted zone `Z06012413HNDFGVBFSYW7`: move or import the staging-owned Route 53, ACM, CloudFront, WAF and SES resources before a separate production state can own them.
8. Obtain a current AWS quote and explicit approval before any billable capacity change.
9. Create or approve an isolated staging/load-test environment. The public staging-labelled stack is not automatically safe for load testing.

## Current implementation position

The application hardening, additive Flyway migrations, review-only Terraform,
guarded k6 project and deployment workflows are local working-tree changes.
Backend verification passed 257 tests; frontend tests/lint/format/build passed.
A clean production-style PostgreSQL 16 run applied all 20 Flyway migrations
through `V25` and passed Hibernate schema validation in 5.5 seconds after
disabling the PostgreSQL transactional Flyway lock required by the concurrent
`V22` indexes. No local Docker image was built because the Docker daemon is
unavailable. Nothing has been committed, pushed, applied or deployed, and no
shared AWS load test has been run.

The remaining work in the P0/P1 sections is staging integration, failure
testing, performance proof and closure of explicitly partial scope—not an
assertion that the local implementations are already production-safe.

## P0: stop correctness and login-wave failures

### Distributed application rate limiting

Local state: implemented and unit/integration tested; not deployed.

- Key normal authenticated limits primarily by immutable authenticated user ID.
- Key login protection by normalized account identifier plus trusted client IP.
- Trust forwarded addresses only from configured CloudFront/ALB proxy boundaries.
- Keep separate distributed-abuse controls without treating a campus NAT IP as one user.
- Return a consistent 429 body and `Retry-After`.
- Add Redis outage behavior, shared-NAT, spoofed-header and concurrency tests.
- Propose a scoped WAF auth rule with metrics; do not change WAF without approval.

Exit evidence:

- legitimate shared-NAT users receive zero incorrect 429 responses;
- credential-stuffing attempts remain bounded; and
- limits remain consistent across multiple backend tasks.

### Authorization snapshots

Local state: implemented and tested; not deployed. Staging must validate
multi-task invalidation and cache-outage behavior.

- Store only the minimum authorization/session snapshot in Valkey.
- Use a short configurable TTL and session/version field.
- Invalidate on password, role, permission, status and session-version changes.
- Fall back safely to PostgreSQL during cache failure.
- Export cache hit, miss, stale-reject and DB-fallback metrics.

Exit evidence:

- no PostgreSQL lookup on the steady-state authenticated request path;
- invalidation tests cover every mutation path; and
- a Valkey outage does not bypass authorization.

### Notice delivery

Local state: implemented with SSE, Redis Pub/Sub and a 90-second unread-count
fallback; not deployed. CloudFront/ALB connection longevity and task-replacement
behavior remain to be tested.

- Add authenticated SSE for unread-count/change events.
- Fan out events across tasks through Valkey Pub/Sub or an approved shared mechanism.
- Add heartbeat, reconnect with jittered exponential backoff and leak cleanup.
- Provide unread-count-only fallback polling every 60–120 seconds while visible.
- Stop fallback polling while the browser tab is hidden.

Exit evidence:

- no 15-second full-inbox polling;
- reconnect works across task replacement; and
- 5,000 open dashboards do not create polling amplification.

## P1: remove database, heap and request-thread bottlenecks

### Database access

Local state: targeted scoped/batched/aggregate queries and `V22` indexes are
implemented. The repository-wide growing-list audit and production-sized
synthetic `EXPLAIN (ANALYZE, BUFFERS)` work remain incomplete. The clean
PostgreSQL 16 migration/Hibernate validation passed locally, but the external
RDS owner has not approved or applied it.

- Replace unbounded `findAll()` and Java-side filtering in growing datasets.
- Enforce server-side pagination and a configurable maximum page size.
- Add scoped projection queries for dashboard, attendance, fees, students and reports.
- Review eager relationships and verify suspected N+1 paths.
- Define query timeouts and a total connection budget.
- Start Hikari sizing from the database budget, task count and worker count; do not multiply an arbitrary pool maximum by twelve tasks.
- Add only evidence-backed indexes through additive Flyway migrations.
- Validate hot queries on production-sized synthetic data using `EXPLAIN (ANALYZE, BUFFERS)` in an isolated database.

### External RDS evaluation

- Compare `db.r7g.large` against the current staging class with the same dataset and workload.
- Evaluate `db.r7g.xlarge` only if the large class fails with an identified resource bottleneck.
- Benchmark RDS Proxy, including transaction/session pinning and added latency.
- Record backup, Multi-AZ, maintenance, downtime and rollback requirements with the RDS owner.
- Do not resize or alter the external production database from application Terraform.

### Direct private file transfer

Local state: the admission-document flow is implemented end to end with
`V23`; other buffered file categories still require inventory/migration.
GuardDuty malware-protection Terraform is review-only and disabled because the
application quarantine/promotion consumer is not complete.

- Introduce tenant-bound upload metadata and object keys.
- Issue short-lived presigned PUT/GET operations.
- Validate size, type, extension and checksum.
- Require completion confirmation before a file becomes visible.
- Define quarantine and malware-scanning state.
- Keep buckets private and deny cross-tenant object access.
- Make all new scanning services subject to cost and apply approval.

### Email and report workers

Local state: queue-backed email and async CSV report jobs are implemented with
a separate ECS worker design. Existing synchronous report exports and the
broader PDF/Excel scope remain; all queue/worker Terraform is disabled and
unapplied.

- Put email and report requests on separate queues.
- Add DLQs, bounded exponential retries and idempotency keys.
- Persist status without logging message bodies or credentials.
- Store report results privately and return short-lived download URLs.
- Add expiration and cleanup.
- Run workers separately from request-serving capacity.

Exit evidence:

- API requests do not wait for SES or report generation;
- retries cannot duplicate a message/report; and
- queue/DLQ alarms reach a confirmed operator.

### Concurrent updates

Local state: selectively implemented for admission forms and fee structures
with `V24`, HTTP 409 handling and overlapping-transaction tests.

- Add optimistic locking only to mutable aggregates with demonstrated lost-update risk.
- Map conflicts to a clear HTTP 409 response.
- Add real two-transaction conflict integration tests.
- Do not add versions to immutable append-only audit/event records.

## P2: capacity and observability

### Candidate ECS model

Prepare, but do not apply, a reviewable model with:

- off-peak minimum two tasks;
- approved scheduled pre-scaling to six or eight before known login windows;
- maximum twelve tasks;
- CPU, memory and ALB requests-per-target policies;
- conservative scale-in and faster scale-out cooldowns;
- deployment circuit breaker and rollback; and
- capacity, deployment and health alarms.

Compare 1-vCPU/2-GiB and 2-vCPU/4-GiB tasks using cost-normalized throughput. Do not assume the larger task is better without profiling and load-test evidence.

The 2–12 model, connection-budget guards, dedicated async worker and circuit
breaker are now represented in local Terraform. They are not an approved
capacity setting and have not changed live staging.

### Required telemetry

- ALB latency and target 5xx;
- application 429;
- Hikari active, idle, pending and timeouts;
- JVM heap and GC;
- ECS CPU, memory, running tasks, deployments and scaling failures;
- RDS CPU, connections, latency, storage and locks;
- Valkey CPU, memory and evictions;
- SQS depth, age and DLQ messages;
- SES sends, rejects, bounces and complaints;
- S3 upload/quarantine outcomes; and
- report/email job success, failure and duplicate prevention.

Confirm an SNS recipient rather than inventing one.

## Delivery sequence

1. Complete local code, tests, migrations, Terraform plans and documentation.
2. Close remaining local integration/workflow review and produce a fresh
   Terraform plan once the external VPC/RDS/state inputs exist.
3. Review security, current cost quote, database ownership and rollback.
4. Request one explicit approval for isolated staging changes.
5. Deploy immutable API/worker/frontend artifacts to isolated staging.
6. Run migrations plus functional, queue recovery, malware-quarantine,
   failure and tenant-isolation tests.
7. Run k6 smoke, then 500, 1,000, 2,500 and 5,000-user gates.
8. Repeat the passing 5,000-user run on the unchanged release candidate.
9. Present evidence, exact production plan, cost, security impact, downtime
   and rollback.
10. Request separate explicit approval for production.

## Definition of done

The project is 5,000-user ready only when the release candidate passes the acceptance criteria twice, all safety alarms remain healthy, no data-integrity or tenant-isolation error occurs, and the production architecture has an approved operating and rollback owner.
