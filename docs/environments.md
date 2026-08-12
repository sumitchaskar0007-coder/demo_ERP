# Environment architecture

Jadhavar ERP supports exactly three runtime environments. A process must activate
one, and only one, of `local`, `preprod`, or `production` through
`SPRING_PROFILES_ACTIVE`. The application fails during startup for mixed, missing,
or retired `docker`/`staging` environment profiles.

## Contract

| Concern | Local | Preproduction | Production |
|---|---|---|---|
| Spring profile | `local` | `preprod` | `production` |
| Purpose | Developer workstation and Docker Compose | Full release rehearsal | Live users and data |
| Frontend | Vite or local Nginx | S3 + CloudFront + WAF | S3 + CloudFront + WAF |
| Backend | JVM or Docker Compose | At least two private ECS API tasks | At least two private ECS API tasks |
| Database | Local PostgreSQL | Isolated RDS PostgreSQL | Isolated production RDS PostgreSQL |
| Cache | Optional/local Redis | Encrypted ElastiCache Valkey | Encrypted ElastiCache Valkey |
| Storage | Local filesystem | Private, versioned S3 | Private, versioned S3 |
| Email | Disabled or Mailpit | SES SMTP through SQS worker | SES SMTP through SQS worker |
| Reports | Database worker | SQS plus dedicated ECS worker | SQS plus dedicated ECS worker |
| Secrets | Local uncommitted `.env` | Preprod Secrets Manager resources | Production Secrets Manager resources |
| Schema | Developer-controlled | One-off Flyway migration task | Approved one-off Flyway migration task |
| Data | Disposable | Synthetic/non-production only | Real production data |

No preproduction resource, secret, queue, bucket, database, cache, DNS name, or
Terraform state may be reused by production.

## Configuration ownership

Application behavior is shared. Environment differences are injected at the
outside boundary:

1. `application.properties` contains environment-neutral defaults.
2. `application-local.properties`, `application-preprod.properties`, and
   `application-production.properties` define each environment contract.
3. ECS task definitions supply non-secret runtime values.
4. Secrets Manager supplies credentials and signing secrets.
5. Terraform creates environment-specific infrastructure.
6. GitHub Actions deploys the same immutable backend image to migration, API,
   and worker tasks.

Feature code must not branch on hostnames, AWS account IDs, or environment names.
Use configuration properties and replaceable service interfaces instead.

## Local

The default profile is `local`. For direct development, copy
`backend/.env.example` to an ignored `backend/.env`, start PostgreSQL, and run the
backend and frontend normally.

Docker Compose also activates `local`, but overrides Flyway, Hibernate, database,
Redis, and bootstrap settings through the root `.env` file:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Use Mailpit when email rendering/delivery needs local testing. Local mode never
uses production AWS resources.

## Preproduction

Preproduction is production parity, not a lightweight development server. Its
Terraform contract requires:

- at least two API tasks;
- a dedicated async worker;
- email and report SQS queues plus dead-letter queues;
- SES SMTP delivery;
- private S3 uploads and presigned transfers;
- PostgreSQL TLS verification;
- encrypted Redis/Valkey;
- exact HTTPS origins and secure cookies; and
- CloudWatch logs, alarms, and readiness checks.

Only synthetic users and approved test recipients belong in preproduction. If
SES is still sandboxed, verify the sender and every recipient used by automated
or manual tests. Test email queue publication, worker consumption, SES delivery,
retry, and DLQ alarms before promoting a release.

The `sai` branch deploys through `.github/workflows/deploy-preprod.yml`. Before
the first deployment using this contract, apply the reviewed preproduction
Terraform plan with `environment="preprod"`, `async_queues_enabled=true`, and
`mail_enabled=true`. This provisions the worker expected by the workflow.

Configure the protected GitHub environment `preprod` with secret
`AWS_DEPLOY_ROLE_ARN` and environment variables `AWS_REGION`, `ECR_REGISTRY`,
`ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `ECS_BACKEND_TASK_FAMILY`,
`ECS_MIGRATION_TASK_FAMILY`, `ECS_WORKER_SERVICE`, `ECS_WORKER_TASK_FAMILY`,
`ECS_SUBNETS`, `ECS_SECURITY_GROUP`, `FRONTEND_BUCKET`,
`CLOUDFRONT_DISTRIBUTION_ID`, and `APPLICATION_URL`. Populate them from the
reviewed preproduction Terraform outputs; do not hard-code AWS identifiers in
the workflow.

## Production

Production continues to activate only `production`. Existing production
properties and the protected `.github/workflows/deploy-ecs.yml` release path are
preserved. Production changes require:

1. a release proven in preproduction;
2. database-owner/Flyway approval;
3. protected `main` branch checks;
4. protected GitHub `production` environment approval; and
5. the exact `DEPLOY_PRODUCTION_ECS` confirmation.

Do not apply preproduction Terraform state or secrets to production. Do not run
automatic administrator bootstrap or Flyway in API/worker replicas.

## Promotion rule

Promote code, never configuration or data:

```text
feature branch -> sai -> preproduction evidence -> main -> production approval
```

The backend Docker digest promoted to production should be the reviewed release
artifact. Environment-specific frontend public settings may be rebuilt, but no
secret may be embedded in a Vite variable.

## Required preproduction evidence

Before production approval, record:

- release commit and backend image digest;
- migration task exit code and Flyway version;
- API and worker task stability;
- login, refresh, logout, role, and tenant-isolation checks;
- direct S3 upload/download checks;
- email outbox -> SQS -> worker -> SES delivery check;
- report queue -> worker -> private S3 download check;
- notice SSE reconnect check;
- readiness and CloudWatch alarm state; and
- tested rollback to the prior API and worker revisions.
