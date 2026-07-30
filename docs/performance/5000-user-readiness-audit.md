# Jadhavr ERP 5,000-user readiness audit

AWS inventory date: 2026-07-28

Documentation reconciliation date: 2026-07-29

Capacity status: **not proven and not ready for a 5,000-user claim**

This document records a point-in-time repository review and read-only AWS inspection. It is not a capacity-test result. No production or shared-environment load test has been run.

## Safety and verification boundary

- The latest detailed AWS inventory is the 2026-07-28 snapshot below.
- A refresh attempt on 2026-07-29 failed because the default AWS login session
  had expired. AWS configuration still identifies account `814645955631`,
  region `ap-south-1` and root principal
  `arn:aws:iam::814645955631:root`, but current resource state could not be
  re-verified.
- Do not refresh or write AWS state with root. Establish a least-privilege IAM
  Identity Center or assumed-role session first, then repeat the complete
  read-only inventory.
- No secret value was read.
- Existing staging resources were not changed.
- The public domain currently routes to the staging-labelled stack. That stack must not be treated as an isolated load-test target until its data, users, integrations and ownership are explicitly verified.
- Production RDS and production network resources are externally owned. Application Terraform must consume their identifiers and must not create a second production VPC, RDS instance, DB subnet group, RDS KMS key or RDS security group.
- The intended production domain is `jadhavaredu.com` in hosted zone
  `Z06012413HNDFGVBFSYW7`. The current staging Terraform state owns related
  Route 53/ACM/CloudFront/WAF/SES resources, so a controlled move/import plan is
  required before a separate production state can manage them.
- Local working-tree implementation described below has not been committed,
  pushed, applied, deployed or enabled in AWS.

## Last verified AWS baseline (2026-07-28)

| Component       | Verified state                                                                                                                                                                 | Readiness observation                                                          |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| Identity        | Account `814645955631`; region `ap-south-1`; root CLI session                                                                                                                  | P0 governance blocker                                                          |
| ECS             | Only `jadhavr-erp-staging`; service `jadhavr-erp-staging-backend`; two healthy Fargate tasks                                                                                   | No production ECS service exists                                               |
| Task size       | 1 vCPU and 2 GiB per task; task definition revision 8                                                                                                                          | Candidate baseline only; capacity unproven                                     |
| Autoscaling     | Minimum 2, maximum 6; CPU target 60%, memory target 70%                                                                                                                        | No scheduled scaling, requests-per-target policy or maximum 12                 |
| ALB             | Only staging ALB/TG; both targets healthy; deletion protection enabled                                                                                                         | Production ALB/TG absent; ALB access logs disabled                             |
| RDS             | Only `jadhavr-erp-staging-postgres`; PostgreSQL 17.5, `db.t4g.medium`, Multi-AZ, private, encrypted gp3, Performance Insights and enhanced monitoring, 14-day backup retention | No production RDS endpoint; no RDS Proxy; default parameter group              |
| Valkey          | Only `jadhavr-erp-staging-redis`; two `cache.t4g.small` nodes, TLS required, encrypted at rest, Multi-AZ and seven-day snapshots                                               | No production cache                                                            |
| CloudFront      | Distribution `E2PWJKUNOR00J0`; apex and `www` aliases; API origin is `api.jadhavaredu.com`                                                                                     | Access logging and behavior compression are disabled                           |
| Route 53        | Zone `Z06012413HNDFGVBFSYW7`; apex/`www` point to CloudFront; API points to the staging ALB                                                                                    | Public traffic is using staging-labelled resources                             |
| WAF             | CloudFront Web ACL `jadhavr-erp-staging`; auth rate rule is 300 requests per five minutes per source IP                                                                        | Shared-campus NAT can receive incorrect 429 responses                          |
| S3              | Staging frontend/upload buckets, Terraform-state bucket and production artifact bucket remain private                                                                          | Production document bucket is absent                                           |
| SES             | Domain, DKIM and custom Mail-From verified                                                                                                                                     | Sandbox: 200/day and 1/second; production-access request is denied             |
| SQS             | No queues in `ap-south-1` or `us-east-1`                                                                                                                                       | Email/report queues and DLQs do not exist                                      |
| SNS             | Topic `jadhavr-erp-staging-alerts` exists                                                                                                                                      | It has zero subscriptions, so alarms notify nobody                             |
| CloudWatch      | Fourteen metric alarms, no dashboard                                                                                                                                           | Coverage is incomplete and one production alarm targets a deleted EC2 instance |
| Secrets Manager | Staging application, Redis, RDS and mail secrets; production EC2 application-secret metadata                                                                                   | Production runtime DB, migration DB and mail secrets are absent                |

Read-only public checks returned HTTP 200 for
`https://jadhavaredu.com/` and an `UP` response from its same-origin
`/api/health` route. A direct
`https://api.jadhavaredu.com/actuator/health/readiness` check timed out. The
successful same-origin check reaches the staging-labelled public stack; it is
not evidence of a production deployment or 5,000-user capacity.

## Confirmed production deletion

CloudTrail recorded an MFA-authenticated root console session from `106.213.84.145` deleting the previously prepared production resources on 2026-07-28:

- production document bucket at approximately 12:01 IST;
- production RDS at approximately 12:20 IST;
- production ALB and target group at approximately 12:45–12:46 IST;
- production EC2 at approximately 12:57 IST; and
- production security groups, subnets, internet gateway and VPC at approximately 12:59 IST.

The deleted backend security group was `sg-00e9897705fbe0c9f`. It must not be given to the RDS owner as an active source because it no longer exists. Investigate/revoke the uncontrolled root session and establish a controlled AWS change identity before creating any replacement.

## Standalone EC2 findings

Two running standalone instances were found, but neither is a production-ready substitute for a multi-AZ backend service:

| Region       | Instance              | Size        | Material gaps                                                                                 |
| ------------ | --------------------- | ----------- | --------------------------------------------------------------------------------------------- |
| `us-east-1`  | `i-081d87cd2c4fa1631` | `t2.medium` | No ALB/ECS/RDS integration, IAM profile, SSM, termination protection or encrypted root volume |
| `ap-south-1` | `i-03420398638c6d2dc` | `t3.large`  | No IAM profile, SSM, termination protection or encrypted root volume; SSH open to `0.0.0.0/0` |

Do not deploy the ERP to either instance until the target architecture, region, network, identity, encryption, backups and high-availability requirements are approved.

## Local implementation checkpoint

The working tree now contains the following safe local changes. The complete
file/test matrix is in `implementation-status.md`.

- Redis-backed user-aware rate limiting with trusted-proxy address resolution,
  consistent 429 responses and tests.
- A minimal Redis authorization snapshot with invalidation, database fallback
  and cache/fallback metrics.
- Authenticated notice SSE with Redis Pub/Sub, an unread-count endpoint and a
  90-second visible-tab fallback in the frontend.
- Targeted scoped/batched database queries plus additive concurrent indexes in
  Flyway `V22`; the full query audit and synthetic `EXPLAIN` evidence remain
  incomplete.
- Tenant-bound admission-document presign/complete/download flows, upload state
  in `V23`, checksum validation and frontend direct PUT support.
- SQS-backed email producer/consumer/recovery code and a separate ECS worker
  design.
- Durable async CSV report jobs for admissions, fees, attendance and students,
  with `V25`, status/download endpoints, private S3 results and worker recovery.
  Existing synchronous exports and the broader PDF/Excel requirement remain.
- Selective optimistic locking for admission forms and fee structures with
  `V24`, HTTP 409 handling and overlapping-transaction tests.
- Review-only ECS, scaling, queue/DLQ, dashboard, alert and CI/CD Terraform and
  workflow changes. GuardDuty malware protection is present but explicitly
  disabled; it is not safe to enable until quarantine promotion is implemented
  and cost/alert approvals exist.

Backend verification passed 257 tests on JDK 17. Frontend unit tests (29),
lint, format checking and production build passed. These are local correctness
signals only. A separate clean production-style PostgreSQL 16 check applied and
validated all 20 Flyway migrations through `V25`, then passed Hibernate schema
validation in 5.5 seconds. That check required
`spring.flyway.postgresql.transactional-lock=false` so Flyway's advisory lock
does not self-block `CREATE INDEX CONCURRENTLY` in `V22`. No Docker image or
shared-environment deployment/load test has validated the combined release,
and no migration was applied to staging or the external production RDS.

## Remaining application and capacity risks

| Priority | Finding                                                                                        | Expected effect at scale                                          | Evidence required to close                                          |
| -------- | ---------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------- |
| P0       | There is no stable production ECS/network/database path                                        | Deployment cannot complete safely                                 | Controlled ECS plan plus external VPC/RDS contract                  |
| P0       | The live WAF still uses source-IP-only auth limiting                                           | Legitimate campus login waves can be blocked                      | Shared-NAT staging test and approved scoped WAF plan                |
| P0       | Local auth/SSE/cache changes are not deployed                                                  | Live request/database/polling behavior is unchanged               | Approved isolated-staging deployment and multi-task failure tests   |
| P0       | SES is sandboxed and the access request was denied                                             | Transactional messages throttle or fail                           | Approved SES production access plus deployed queue worker           |
| P1       | Live ECS remains maximum six tasks                                                             | Insufficient headroom if one-vCPU tasks remain                    | Cost/DB-budget review and load evidence for the prepared 2–12 model |
| P1       | Only staging `db.t4g.medium` exists and production RDS is externally absent                    | No production data path; burst/connection risk remains unmeasured | External contract and synthetic benchmark owned with the RDS team   |
| P1       | No RDS Proxy exists                                                                            | Connection storms may occur during scaling                        | Pinning/latency benchmark before adoption                           |
| P1       | Async code exists locally, but no SQS queues, worker service or report-result path is deployed | API isolation and retry behavior are unproven operationally       | Queue/worker staging deployment, alarms and failure testing         |
| P1       | Admission presigning is local only and other buffered file paths remain                        | Heap/thread pressure remains possible                             | Staging transfer tests plus inventory/migration of remaining paths  |
| P1       | Malware protection is disabled and no promotion consumer is implemented                        | A completed upload is not malware-gated                           | Quarantine state machine, clean/threat tests, quote and approval    |
| P1       | ALB and CloudFront access logs are disabled                                                    | Weak incident and latency investigation evidence                  | Approved logging destination and retention                          |
| P1       | Alert topic has no subscribers                                                                 | Operational failures are not delivered                            | Confirmed recipient and subscription approval                       |
| P1       | No live CloudWatch dashboard exists                                                            | Test results cannot be correlated quickly                         | Apply reviewed observability in isolated staging                    |
| P2       | CloudFront behavior compression is disabled                                                    | More bandwidth and slower asset/API transfer                      | Verify asset compression policy and response headers                |
| P2       | Standalone EC2 volumes are unencrypted and unmanaged                                           | Security, recovery and HA exposure                                | Keep EC2 out of the authoritative production design                 |

## Monitoring gaps

The live alarms cover basic ECS CPU/memory scaling, running-task count, ALB/target 5xx, unhealthy targets, RDS CPU/connections/storage and Valkey CPU. Local Terraform prepares additional dashboard/queue/capacity alarms, but none has been applied. The following signals remain absent live or incomplete locally:

- read and write p95/p99 latency;
- application 429 outcomes;
- Hikari active, idle, pending and timeout metrics;
- JVM heap, GC pause and process restart;
- ECS deployment and scaling failures;
- RDS read/write latency, locks and replication lag where applicable;
- Valkey memory pressure and evictions;
- SQS depth, oldest age and DLQ messages (prepared locally, absent live);
- SES rejection, bounce and complaint rates;
- S3 upload/quarantine failures (GuardDuty alert plumbing is disabled and the
  application promotion path is missing); and
- background-job failure and duplicate outcomes.

Container Insights retains only one day of performance logs. The appropriate retention period must be selected before a test so evidence is not lost.

## Strengths worth preserving

- Staging RDS is private, encrypted, Multi-AZ, deletion-protected and monitored.
- Staging Valkey is private, encrypted, TLS-required and Multi-AZ.
- ECS has two healthy tasks and deployment circuit-breaker rollback.
- The frontend S3 origin and upload bucket are private.
- CloudFront uses TLS 1.2 or later and an origin-access control.
- Backend images use immutable ECR tags and scan on push.
- The staging target group uses a readiness health endpoint.

## Readiness decision

Jadhavr ERP must not be described as supporting 5,000 concurrently active users. That decision remains blocked until:

1. an isolated staging/load-test environment exists;
2. synthetic data and at least one unique synthetic account per peak virtual user are available;
3. required application and observability work is deployed with approval;
4. the 500, 1,000 and 2,500-user gates pass;
5. two repeatable 5,000-user release-candidate runs meet every acceptance criterion; and
6. AWS and application metrics show no hidden saturation, tenant leakage, failed jobs or integrity errors.
