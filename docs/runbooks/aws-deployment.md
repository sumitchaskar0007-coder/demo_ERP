# AWS production deployment runbook

Status date: 2026-07-29
Deployment status: **blocked; no production apply or deployment has run**

The authoritative production backend is the ECS/Fargate service behind an ALB
defined in `infra/terraform`. The API runs with at least two private tasks when
activated. Email and report work runs in the separate private ECS worker when
async queues are enabled. The frontend remains private S3 behind CloudFront.

`infra/terraform-ec2`, `scripts/ec2` and `.github/workflows/deploy.yml` are a
legacy review-only path. A raw single EC2 instance is non-HA and must not be
used as the production or rollback architecture for the 5,000-user objective.

This runbook is not authorization. Do not run `terraform apply`, deploy,
trigger a workflow, change DNS/IAM/WAF/SES/certificates/secrets, or run a shared
load test without the explicit gates in the production-hardening specification.

## Current blockers

The latest detailed read-only inventory is the 2026-07-28 snapshot. It found:

- a default AWS configuration for account `814645955631`, region
  `ap-south-1`, and root principal `arn:aws:iam::814645955631:root`;
- no production VPC, production ECS service/ALB or production RDS instance;
- no production RDS security group, runtime database secret or migration
  database secret;
- no live email/report SQS queues or async worker;
- SES still in the sandbox;
- an SNS alert topic with zero subscriptions;
- `collegeerp.example` currently served by staging-labelled CloudFront/ALB
  resources; and
- GitHub authentication/protected-environment configuration not ready for a
  production run.

A refresh attempt on 2026-07-29 failed because the default root login had
expired, so none of the resource state above was re-verified that day.

A production Terraform plan cannot be trusted until the missing external
values and state-ownership decisions are supplied and the full read-only
inventory is repeated through a least-privilege SSO/assumed-role login. Do not
substitute deleted, stale or screenshot-only IDs.

## Ownership boundary

For `environment = "production"`, `infra/terraform` must consume, and must not
create, the following externally owned resources:

- production VPC and public/backend/cache subnets;
- production RDS instance/cluster and endpoint;
- RDS DB subnet group, RDS KMS key and RDS parameter group;
- production RDS security group;
- runtime database role and Secrets Manager secret; and
- migration database role and Secrets Manager secret.

The existing staging behavior is intentionally unchanged: staging Terraform
continues to manage its staging VPC and staging RDS resources.

The RDS owner must allow PostgreSQL TCP/5432 only from Terraform output
`backend_security_group_id`. The application Terraform does not add ingress to
the external RDS security group. The RDS owner also owns the production
database roles, backups, monitoring, parameter changes and Flyway approval.

Application Terraform may create the production ECS/ALB/Valkey/S3/CloudFront
application resources, application security group, task roles, queues,
dashboard and alarms only after their reviewed plan is approved. Its upload
KMS key is for application objects and is not an RDS KMS key.

## Required external inputs

- AWS Region: `ap-south-1`
- production VPC: `production_vpc_id`
- at least two public ALB subnets across AZs:
  `production_public_subnet_ids`
- at least two private ECS subnets across AZs:
  `production_backend_subnet_ids`
- optional separate private cache subnets:
  `production_cache_subnet_ids`
- RDS endpoint: `production_rds_endpoint`
- database port: `production_database_port` (must be `5432`)
- database name: `production_database_name`
- production RDS SG: `production_rds_security_group_id`
- runtime DB secret ARN: `production_runtime_database_secret_arn`
- distinct migration DB secret ARN:
  `production_migration_database_secret_arn`
- any customer-managed secret key ARNs:
  `production_database_secret_kms_key_arns`

Both DB secrets must be created by the RDS owner and contain string fields
named `username` and `password`. Do not put their values in Terraform,
GitHub, chat, shell history, images or logs.

Before planning, also obtain:

- the alert recipient;
- current pricing/cost approval;
- the exact GitHub repository and protected-environment owners;
- the Terraform backend configuration and state-move/import plan; and
- the approved ACM/Route 53/CloudFront/WAF/SES ownership plan for
  `collegeerp.example` and hosted zone `Z06012413HNDFGVBFSYW7`.

## Terraform

Use `infra/terraform/backend.production.hcl.example` as a template for a local,
ignored production backend file. The production key must remain separate from
the historical staging state. Never let two states manage the same Route 53,
ACM, CloudFront, WAF, SES or account-global GitHub OIDC resource.

```sh
cp infra/terraform/backend.production.hcl.example \
  infra/terraform/backend.production.hcl
cp infra/terraform/production.external.tfvars.example \
  infra/terraform/production.external.tfvars
```

Replace every placeholder in those ignored files with verified, current
values. Keep this first review configuration:

```hcl
production_database_access_ready = false
desired_count                     = 0
backend_peak_schedule_enabled     = false
async_queues_enabled              = false
mail_enabled                      = false
malware_protection_enabled        = false
alert_email                       = ""
```

Run local formatting/validation, then initialize the separate state and create
a saved plan:

```sh
terraform fmt -check -recursive infra
terraform -chdir=infra/terraform init -backend=false -input=false
terraform -chdir=infra/terraform validate

export TF_DATA_DIR="$PWD/infra/terraform/.terraform-production"
terraform -chdir=infra/terraform init \
  -reconfigure \
  -backend-config=backend.production.hcl
terraform -chdir=infra/terraform plan \
  -var-file=production.external.tfvars \
  -out=production.tfplan
```

Planning is read-only with respect to remote resources, but it still needs
working least-privilege credentials and all external data-source IDs. Review
the saved plan for exact creates/updates/deletes, replacement risk, state
ownership, expected downtime, monthly cost, security impact and rollback.

Do not apply as part of this procedure until that packet has explicit staging
approval. Never use `-auto-approve`. A later approved apply uses only the
reviewed saved plan:

```sh
terraform -chdir=infra/terraform apply production.tfplan
```

That command is intentionally shown for the approved change window; this
document does not grant permission to run it.

## Production activation gate

Use two separately reviewed infrastructure phases.

### Phase A: zero-task infrastructure handoff

The RDS owner first creates the external RDS security group without application
ingress and supplies its ID with the other external contract values. Keep
`production_database_access_ready=false` and `desired_count=0`. After an
explicitly approved apply, record these outputs:

- `backend_vpc_id`
- `backend_subnet_ids`
- `backend_security_group_id`
- `ecs_execution_role_arn`
- `application_secret_arn`
- `mail_secret_arn`
- `ecr_repository_url`
- `ecs_cluster_name`
- `ecs_service_name`
- `migration_task_definition`
- `github_deploy_role_arn`

The RDS owner then permits TCP/5432 only from
`backend_security_group_id`. If the external secrets use customer-managed KMS
keys, their key policies must authorize the output ECS execution role for the
required decrypt path.

This first apply still creates cost-bearing application infrastructure. It is
not a workaround for the cost/approval gate.

### Secrets

Terraform creates only secret containers for application/mail data in
production. Populate their values directly in Secrets Manager using an
approved operator; never send values here or place them in tfvars.

Required application keys include:

- `JWT_SECRET`
- `RATE_LIMIT_KEY_SECRET`

`REDIS_PASSWORD` is supplied separately from the Terraform-managed Valkey
secret; do not duplicate it in the application secret.

Required mail keys, when email is approved, include:

- `MAIL_HOST`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM_ADDRESS`

Do not set `mail_enabled=true` until SES production access and a controlled
delivery test are approved. Production Terraform requires async queues when
mail is enabled so API replicas do not perform SMTP delivery.

### Database and Flyway handoff

Before activation:

1. The RDS owner confirms the runtime and migration roles are separate and
   least privilege.
2. Both secret ARNs exist and are decryptable by the approved task execution
   role without exposing their values.
3. The RDS SG allows 5432 only from `backend_security_group_id`.
4. The RDS owner reviews additive migrations `V22` through `V25`, backup/PITR,
   connection budget, the PostgreSQL Flyway transactional-lock setting and
   rollback.
5. The RDS owner either runs Flyway with its controlled process or records
   explicit approval for the protected ECS workflow to run the migration task
   using the separate migration secret.
6. Migration success is recorded before the API and worker release proceeds.

API and worker task definitions always use Flyway/bootstrap disabled. Only the
one-off migration task enables Flyway. Production bootstrap/database-role
creation remains externally owned.

Local evidence: a clean PostgreSQL 16 production-style run applied and
validated all 20 migrations through `V25` and passed Hibernate schema
validation in 5.5 seconds. The application sets
`spring.flyway.postgresql.transactional-lock=false`; without it, Flyway's
transactional advisory lock self-blocks the `CREATE INDEX CONCURRENTLY`
statements in `V22`. This local result does not authorize or prove the external
RDS migration. The RDS owner must review the setting and execute/approve the
production migration separately.

### Phase B: application activation

Only after the RDS handoff and a new approved plan:

```hcl
production_database_access_ready = true
desired_count                     = 2
```

The ECS service preconditions require at least two API tasks and enforce the
aggregate API-plus-worker Hikari connection budget. Peak scheduling remains
off until separately justified. Enable async queues, email and malware
protection independently; none is implied by database activation.

When `async_queues_enabled=true`, Terraform prepares:

- separate email/report queues and DLQs;
- API roles limited to queue publishing;
- a dedicated worker task/service with queue consumer and private result-object
  permissions;
- independent worker Hikari sizing; and
- queue-depth, oldest-message and DLQ alarms.

The email worker currently requires exactly one task for durable recovery until
leader election exists. Report database polling remains enabled in the worker
to recover stale/retry jobs.

`malware_protection_enabled` must remain false until the application
clean/threat/quarantine promotion consumer, alarm recipient and exact current
cost are approved and tested. Terraform alerting alone is not a complete
malware gate.

## GitHub Actions production environment

The authoritative workflow is `.github/workflows/deploy-ecs.yml`.
`.github/workflows/deploy.yml` is the manual legacy EC2 path and must remain
unused for the ECS production architecture.

The ECS workflow is manual-only, runs only from `main`, requires the exact
confirmation `DEPLOY_PRODUCTION_ECS`, requires an external RDS/Flyway approval
reference and uses the protected GitHub environment `production`. It uses
GitHub OIDC; do not create long-lived AWS access keys.

Create or update the protected GitHub environment named `production` with:

- Secret `AWS_DEPLOY_ROLE_ARN` — Terraform output
  `github_deploy_role_arn`.
- Variable `AWS_REGION` — `ap-south-1`.
- Variable `ECR_REPOSITORY` — repository name, not its full URL.
- Variable `ECS_CLUSTER` — output `ecs_cluster_name`.
- Variable `ECS_SERVICE` — output `ecs_service_name`.
- Variable `ECS_MIGRATION_TASK_DEFINITION` — a reviewed,
  revision-qualified production migration task definition. Terraform output
  `migration_task_definition` is the family name; resolve and review the exact
  active revision before configuring this variable.
- Optional variable `ECS_WORKER_SERVICE` — output
  `ecs_async_worker_service_name` when async queues are approved/enabled; leave
  empty only when no worker service exists.
- Variable `FRONTEND_BUCKET` — existing live frontend bucket.
- Variable `CLOUDFRONT_DISTRIBUTION_ID` — existing live distribution.
- Variable `APPLICATION_URL` — an HTTPS URL that reaches this exact production
  ECS release, never the old staging service.
- Variable `FRONTEND_URL` — the production CloudFront/domain URL.

Require named reviewers, block self-approval where supported and restrict
deployment branches to protected `main`. Register GitHub OIDC ownership in
exactly one Terraform state; the production example consumes the provider
owned by the historical staging state.

The workflow:

1. re-runs backend tests and the PostgreSQL migration task, plus frontend and
   Terraform validation;
2. builds and scans an immutable backend image and resolves its ECR digest;
3. requires a stable, private, multi-AZ Fargate API with at least two tasks;
   when a worker is configured, it must be stable/private and use the identical
   subnet/security-group set;
4. registers the approved migration definition with that exact digest, runs
   one task and requires the migration command, Flyway enabled, bootstrap
   disabled, the expected image digest and exit code zero; the RunTask client
   token and `startedBy` are stable for the commit plus approved source
   revision, and an identical release payload reuses the latest matching task
   definition so same-approval reruns reconcile the original task during the
   ECS idempotency window; status is reconciled for up to 30 minutes;
5. deploys the API task revision and verifies every running task uses the exact
   digest/release ID with Flyway/bootstrap disabled, rolling back and validating
   the prior revision on failure;
6. when configured, deploys the worker revision and verifies every worker task
   uses the same exact digest with Flyway/bootstrap disabled, rolling back and
   validating the prior revision on failure; if worker preparation/deployment
   fails after the API succeeds, a compensating step attempts to restore both
   services to their prior revisions; and
7. syncs the built frontend, waits for invalidation and verifies frontend plus
   same-origin API health.

The workflow has not been executed. Validate its final YAML and permissions
before it is pushed. A non-empty migration approval string is a technical
guard, not a substitute for the recorded external RDS owner approval.

Cleanup cannot be guaranteed after a runner `SIGKILL` or platform-level
cancellation. A migration that exceeds 30 minutes is reported by ARN and the
same commit/approval must not be retried until the task is reconciled; changing
the approved source revision intentionally produces a new token. The deployment
role cannot call `ecs:StopTask`. Required reviewers, protected-branch
restrictions, self-review prevention and admin-bypass policy must be configured
on the GitHub `production` environment because YAML cannot enforce them.

## Domain cutover

The target domain is `collegeerp.example`, with `www` and
`api.collegeerp.example`, in hosted zone `Z06012413HNDFGVBFSYW7`. It currently
routes to staging-labelled resources. Before a production state/apply:

1. inventory the current Route 53 records, ACM certificates, CloudFront
   distribution, WAF Web ACL, SES identity and their state addresses;
2. choose a single owning state and prepare reviewed `terraform state mv` /
   import operations;
3. prove the production CloudFront/ALB origin and certificate path without
   changing public DNS;
4. ensure `APPLICATION_URL` reaches the new ECS release rather than the old
   staging backend; and
5. obtain separate explicit approval for the state move and DNS cutover.

Do not create duplicate records/distributions or let staging and production
states manage the same resource. Keep the current public route unchanged until
the new API has at least two healthy targets and the release health response
contains the expected 12-character Git release.

After the approved cutover, verify:

```sh
curl --fail --silent --show-error \
  https://api.collegeerp.example/actuator/health/readiness
curl --fail --silent --show-error \
  https://collegeerp.example/api/health
```

Also test login/refresh/logout, tenant isolation, SSE reconnect, private
presigned transfers, queue processing and alarm delivery. Never run the
5,000-user suite as a cutover smoke test.

## Rollback

Record the prior API task definition, worker task definition, image digest,
frontend artifact and Terraform state version before change.

- ECS circuit breakers and the deployment workflow restore the previous API or
  worker task definition on normal shell errors/signals and verify the exact
  prior revision. The compensating step attempts to keep API and worker on one
  coherent release.
- A runner hard-kill can bypass traps. Before retrying, reconcile the migration
  task ARN and both ECS services manually with read-only commands; never assume
  cleanup occurred.
- If the migration succeeds but application rollout fails, keep additive
  `V22`–`V25` schema changes. Never reverse a completed forward-only migration
  ad hoc; deploy backward-compatible code or an RDS-owner-approved corrective
  migration.
- Stop queue producers before workers if a job contract is unsafe. Preserve
  main-queue and DLQ messages for reconciliation.
- Restore the previous versioned frontend and invalidate only required paths.
- DNS rollback requires the prior record values, state ownership and separate
  approval. Do not point at either standalone EC2 instance.
- Never delete/recreate the external production VPC/RDS/SG/KMS/subnet group or
  modify staging RDS as a rollback shortcut.

See `docs/performance/rollback-plan.md` for application-specific cache, SSE,
queue, presigned-transfer, malware and load-test abort procedures.

## Approval packet

Immediately before any staging or production write, present:

- exact resources and Terraform plan summary;
- current monthly cost delta and maximum test-window spend;
- expected downtime and migration sequence;
- security/IAM/DNS/state impact;
- external VPC/RDS/secret ownership confirmation;
- tested rollback procedure;
- alert recipient and operator;
- staging functional/load-test evidence; and
- immutable release commit/image digest.

Request one explicit approval for isolated staging. After staging validation
and all staged load gates pass, request a separate explicit production
approval. Until then, stop after local validation and plan review.
