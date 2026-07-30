# Jadhavar ERP — AWS Deployment and Readiness Report

**Audit date:** 27 July 2026
**Commit reviewed:** `cf694ec` (`main`) plus the corrections listed below
**Current deployed staging target:** AWS `ap-south-1`, CloudFront + WAF + S3 frontend, ALB + ECS
Fargate backend, RDS PostgreSQL, ElastiCache Valkey, Secrets Manager, Route 53, ACM, and CloudWatch.

## Decision

The audited application is deployed and healthy at **https://jadhavaredu.com** in AWS account
`814645955631`, region `ap-south-1`. The Terraform environment remains named `staging` because the
deployment began with a temporary CloudFront domain before the supplied Route 53 domain was
attached.

Deployment verification completed:

- Route 53 and ACM serve `jadhavaredu.com` and `www.jadhavaredu.com`;
  `api.jadhavaredu.com` is the CloudFront ALB origin.
- Both ECS Fargate tasks and both ALB targets are healthy.
- Public readiness returns `{"status":"UP"}`.
- A live Secrets Manager-backed administrator login succeeds with the `SUPER_ADMIN` role.
- The final Terraform plan reports no changes.

AWS SES SMTP infrastructure is configured and its SMTP credential is stored only in Secrets
Manager. Domain verification, DKIM, SPF, DMARC, custom MAIL FROM, and an AWS mailbox-simulator SMTP
test all pass. AWS denied production-access case `178513625800051`, so automatic delivery remains
disabled; enabling it in the SES sandbox would reject unverified student addresses. A reachable
operational/contact email is required before resubmitting. The SNS email subscription also remains
disabled because no operational alert email was supplied. GitHub Actions is connected through
OIDC, but GitHub currently refuses to start hosted runners because of the account's failed payment
or Actions spending limit.

## Production external-RDS handoff

Production has a separate ownership model and has not been applied from the current staging state:

- The supplied production VPC is `vpc-0d7999d87f213a1dc`.
- The RDS owner controls the production RDS instance, subnet group, security group, KMS key,
  parameter group, database roles, Flyway, backups, and monitoring.
- Production Terraform consumes the external endpoint, port, database name, RDS security-group ID,
  runtime secret ARN, and migration secret ARN. It does not create or modify production VPC/RDS
  resources.
- An isolated production plan reports `59 to add, 0 to change, 0 to destroy`; its explicit
  forbidden-resource check reports none. The plan was not applied.
- Phase one keeps ECS at zero tasks. Terraform output `backend_security_group_id` becomes the exact
  source security-group ID that the RDS owner permits on TCP/5432. That AWS ID does not exist until
  phase one is applied.
- The current historical state key remains the live staging state. Production must use
  `backend.production.hcl` and the separate `production-external/terraform.tfstate` key.

## Corrections applied during this audit

- Renumbered the conflicting Flyway migration
  `V12__class_teacher_student_identifiers.sql` to
  `V21__class_teacher_student_identifiers.sql`. The repository previously contained two `V12`
  migrations, which prevents reliable Flyway validation.
- Formatted the five frontend files that failed the Prettier CI gate.
- Formatted `infra/terraform/ecs.tf`, which failed `terraform fmt -check`.
- Added `.terraform/` to `.gitignore` so downloaded providers are not accidentally committed.
- Upgraded Netty to `4.1.136.Final` and PostgreSQL JDBC to `42.7.12` to remove five fixable
  high-severity container findings.
- Created and initialized the encrypted, versioned, publicly blocked Terraform state bucket
  `jadhavr-erp-terraform-state-814645955631-ap-south-1`.
- Added temporary/default CloudFront domain support, then attached the supplied Route 53 domain and
  ACM certificates without replacing data resources.
- Restricted ALB origin access to CloudFront addresses plus a generated secret origin header.
- Added an explicit zero-task bootstrap sequence, a least-privilege database-role task, and a
  migration task that exits cleanly after Spring runners complete.
- Corrected the read-only ECS filesystem configuration so non-root Java/Tomcat processes use a
  secure writable tmpfs at `/app/tmp`.
- Made SMTP independently configurable so deployment can start safely with mail disabled and
  without placeholder mail secrets.
- Added Terraform-managed SES domain authentication, a custom MAIL FROM domain, DKIM/SPF/DMARC,
  least-privilege SMTP credentials, and a dedicated encrypted Secrets Manager secret.
- Added a GitHub OIDC deployment role restricted to this repository's production environment and
  configured the production deployment environment.
- Corrected the Valkey CPU alarm to monitor both emitted `CacheClusterId` metrics; the previous
  replication-group dimension did not emit `EngineCPUUtilization` data.

## Verified gates

| Gate | Result |
|---|---|
| Backend Java 17 tests/package | Pass — 166 tests, 0 failures, 0 errors |
| Empty PostgreSQL migration | Pass — 16 migrations applied from empty schema through version 21 |
| Frontend unit tests | Pass — 17 tests in 6 files |
| Frontend browser smoke | Pass — login route renders in Chromium |
| Frontend TypeScript/Vite build | Pass |
| Frontend Prettier check | Pass |
| Frontend ESLint | Pass with 21 warnings |
| Terraform formatting | Pass |
| Terraform initialization/validation | Pass |
| Backend OWASP dependency scan | Inconclusive — public NVD update was stopped after it began processing 370,431 records without an API key |
| Docker image build | Pass |
| Trivy backend image scan | Pass — 0 high, 0 critical vulnerabilities |
| Live AWS identity | Pass — account `814645955631`, region `ap-south-1` |
| Terraform plan/apply | Pass — deployed; final plan reports no changes |
| Live ECS/ALB health | Pass — 2/2 tasks running and 2/2 targets healthy |
| Public readiness | Pass — `https://jadhavaredu.com/actuator/health/readiness` is UP |
| Authenticated smoke | Pass — Secrets Manager-backed Super Admin login succeeded |
| SES domain authentication | Pass — identity, DKIM, and custom MAIL FROM report SUCCESS |
| SES SMTP smoke | Pass — authenticated delivery to the AWS mailbox simulator |
| SES production access | Blocked — AWS denied case `178513625800051`; resubmit after a reachable contact email is supplied |

## Application module analysis

| Module | Current assessment |
|---|---|
| Authentication and sessions | Good foundations: HttpOnly cookies, refresh rotation, CSRF, explicit CORS, rate limiting, security headers, mandatory production secrets, and authorization tests. |
| Colleges, departments, users, and staff | Tenant-aware service rules and role restrictions are covered by backend tests. Principal/Super Admin boundaries are explicitly tested. |
| Admissions | Workflow and authorization coverage are strong. Upload validation is centralized and signature-aware. Large workflow surface still warrants role-based end-to-end tests before launch. |
| Fees | Payment verification and locking rules have automated coverage. Reports are tenant-scoped and CSV values are formula-neutralized. |
| Academics | Legacy endpoints now have method-level role restrictions and tenant visibility checks. Several list operations still aggregate in Java and should be converted to scoped database queries as data grows. |
| Timetable and attendance | Authorization and approval behavior are tested. The duplicate migration introduced by the class-teacher identifier feature is corrected. |
| Reports and audit | Role and tenant scoping are in place. Exports are capped at 10,000 rows and attendance has a dedicated export path. |
| Email and account recovery | Token and notification services have tests. SES credentials are stored in a dedicated Secrets Manager secret and an authenticated simulator delivery passes. Delivery remains disabled until AWS grants production access for arbitrary student recipients. |
| Object storage | Production uses a private, versioned S3 bucket and task-role credentials; no static AWS keys are required. S3 service behavior has unit tests. |
| Frontend | Build and tests pass. Route splitting improved the main chunk, but spreadsheet and main application chunks remain above 500 kB. Hook dependency/Fast Refresh warnings remain. |

## AWS architecture assessment

### Good controls

- The current staging stack uses two-AZ public, application, and isolated data subnets.
- ECS tasks have no public IP; staging RDS and Valkey accept traffic only from the ECS security
  group. Production RDS access is delegated to its external owner.
- ALB accepts HTTPS only from the AWS CloudFront origin-facing prefix list.
- The current staging RDS uses encryption, Multi-AZ, backups, deletion protection, final snapshots,
  enhanced monitoring, and PostgreSQL logs. The production RDS owner is responsible for equivalent
  production controls.
- Valkey uses TLS, authentication, encryption at rest, Multi-AZ, and automatic failover.
- Frontend and uploads buckets block public access; uploads use versioning and lifecycle cleanup.
- Runtime tasks are non-root, read-only, and use temporary task-role credentials.
- Runtime and migration database identities are separated.
- Production ECS services disable automatic Flyway and administrator bootstrap. The application
  workflow requires external migration approval and never runs production Flyway.
- GitHub deployment uses OIDC, immutable ECR tags, external migration approval, ECS circuit breaker
  rollback, CloudFront invalidation, and a readiness check.

### Remaining improvements

1. Add ECS target-tracking autoscaling for CPU, memory, and/or ALB request count. Current capacity is
   fixed at two tasks.
2. Promote the AWS Common Rules and SQL injection WAF groups from count mode only after reviewing
   sampled staging traffic.
3. Make `alert_email` mandatory for production or integrate the SNS topic with the operational
   incident channel. The current default silently creates no subscription.
4. Add CloudFront, ECS CPU/memory, Redis memory/eviction, and application business alarms; obtain
   production RDS failover/monitoring evidence from the RDS owner.
5. Coordinate a scheduled production RDS restore drill with the RDS owner and record recovery
   time/recovery point objectives.
6. Add a production canary that logs in with a synthetic account and verifies one authenticated
   read path; the current post-deploy check only verifies readiness.

## Functional issues that are not infrastructure blockers

- Teacher dashboard `mySubjects` counts every subject in the teacher's department rather than the
  authenticated teacher's assignments.
- Several dashboard values are placeholders set to zero: HOD classes today, teacher sections/classes/
  attendance work, and student attendance percentage.
- Frontend ESLint reports 21 hook dependency/Fast Refresh warnings.
- Browser coverage currently has one smoke test. Add role-based admission, payment, timetable,
  attendance, and logout/refresh flows before broad production rollout.
- Some academic analytics/list paths still use full-table reads and in-memory filtering.

## Remaining operational follow-up

1. Supply a reachable contact email, resubmit the denied SES production-access request, and after
   approval set `mail_enabled = true`, deploy the resulting ECS task definition, and test delivery
   to an authorized real recipient.
2. Supply an operational `alert_email` and confirm the SNS subscription.
3. Resolve the GitHub account payment/Actions spending-limit block and rerun pull request checks.
4. Resolve or document the non-applicable React Router RSC advisory so the npm audit CI gate is
   deterministic.
5. Retrieve the initial administrator password directly from Secrets Manager, sign in, and rotate
   it through the application.
