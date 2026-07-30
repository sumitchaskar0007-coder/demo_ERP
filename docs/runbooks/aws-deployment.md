# AWS deployment runbook

This runbook assumes the Terraform stack in `infra/terraform` and an ECS
cluster with the backend task definition. Never put credentials or secret
values in commands, Terraform variables, logs, or image layers. Use an
assumed deployment role and Secrets Manager.

## GitHub Actions production environment

The production deployment workflow is `.github/workflows/deploy.yml`. It runs
automatically after the `Verify` workflow succeeds on `main` and can also be
started manually. Create a protected GitHub environment named `production`,
require deployment approval as appropriate, and configure the following
repository or environment variables:

- `AWS_REGION` — for example, `ap-south-1`
- `ECR_REPOSITORY` — ECR repository name, not the full registry URL
- `ECS_CLUSTER` — Terraform output `ecs_cluster_name`
- `ECS_SERVICE` — Terraform output `ecs_service_name`
- `FRONTEND_BUCKET` — Terraform output `frontend_bucket`
- `CLOUDFRONT_DISTRIBUTION_ID` — the production distribution ID
- `APPLICATION_URL` — Terraform output `application_url`

Configure one GitHub environment secret:

- `AWS_DEPLOY_ROLE_ARN` — an AWS IAM role trusted through GitHub OIDC and
  attached to the Terraform output `deployment_policy_arn`

Restrict the role trust policy to this repository and the `production`
environment. Do not create long-lived AWS access-key secrets in GitHub.

Before the first task starts, provide separate externally managed runtime and
migration database secret ARNs. Each secret JSON object must contain
`username` and `password`. Populate the Terraform-created application secret
with `JWT_SECRET` and `RATE_LIMIT_KEY_SECRET`. Terraform stores generated SMTP
credentials in a separate mail secret. Store values through an approved
secret-management channel; do not put JSON in shell history. The application
deployment workflow never reads the migration secret or runs Flyway.

Set both `domain_name` (frontend/CloudFront) and `api_domain_name` (ALB/API).
The regional ACM certificate covers `api_domain_name`; CloudFront uses that
hostname for its HTTPS backend origin.

## Release order

### 5,000-user hardening approval gate

The changes under `docs/performance`, `load-tests/k6`, and the capacity
Terraform resources are proposals until an isolated staging plan is reviewed.
Before applying them:

1. Stop using the AWS account root identity and assume the approved
   least-privilege deployment role.
2. Obtain an operator email for the SNS topic; do not deploy alarms with no
   confirmed recipient.
3. Produce a fresh `terraform plan` for the correct staging state and attach
   the cost worksheet, resource list, downtime expectation and rollback plan.
4. Obtain explicit staging approval before `terraform apply` or a deployment.
5. Run the 500/1,000/2,500/5,000 k6 ladder only against isolated staging.
6. Require two passing 5,000-user runs before requesting a separate
   production approval.

The `sumit` branch is verified by CI but does not automatically deploy.
Production deployment remains restricted to a successful `Verify` run on
`main` and the protected GitHub `production` environment.

0. Apply only after the production RDS owner supplies the endpoint, port,
   database name, RDS security-group ID, and database secret ARNs. Confirm the
   Terraform plan creates no production VPC or RDS infrastructure. Use a new
   production backend key/state; never reuse the historical staging state.
   First apply with `temporary_domain=true` and `desired_count=0`, then send outputs
   `backend_security_group_id` and `ecs_execution_role_arn` to the RDS owner.
   They allow TCP/5432 from that security group, authorize their Secrets
   Manager/KMS policies, and approve Flyway. Then set
   `production_database_access_ready=true` and `desired_count>=2`.
1. Build and push an immutable image tag (or digest) to the ECR repository.

From the repository root, after Terraform has created the ECR repository and
your AWS CLI is authenticated, run:

```sh
AWS_REGION=ap-south-1 ./scripts/push-backend-ecr.sh
```

The script uses the current Git commit as the image tag. To use an explicit
tag or repository URI, set `IMAGE_TAG` or `ECR_REPOSITORY_URI`. It does not
read or print application secrets.
2. Upload the frontend build to the private frontend bucket.
3. Apply Terraform for infrastructure changes and invalidate CloudFront.
4. Obtain explicit confirmation from the RDS owner that their Flyway process
   completed successfully. The application GitHub workflow does not run it.
   Do not deploy a schema-dependent backend revision without that approval.
5. Update the ECS service to the immutable backend task definition. The ECS
   deployment circuit breaker rolls back a failed deployment.
6. Confirm ALB target health and the public health endpoint through the same
   CloudFront hostname. Run the smoke suite before approving production.

## Rollback

Select the previous known-good ECS task-definition revision and update the
service. Do not roll back an already-applied forward-only migration; deploy a
backward-compatible application revision or a corrective migration instead.

```sh
aws ecs update-service --cluster "$ECS_CLUSTER" --service "$ECS_SERVICE" \
  --task-definition "$PREVIOUS_TASK_DEFINITION" --force-new-deployment
aws ecs wait services-stable --cluster "$ECS_CLUSTER" --services "$ECS_SERVICE"
```

## Access and health checks

The ALB accepts traffic only from CloudFront origin-facing addresses. ECS
tasks are private and have no public IP. The externally managed RDS security
group must allow port 5432 only from Terraform output
`backend_security_group_id`; the Redis security group follows the same
application-source pattern. Check
`/actuator/health`, `/actuator/health/liveness`, and
`/actuator/health/readiness`; no other actuator endpoint should be exposed.
