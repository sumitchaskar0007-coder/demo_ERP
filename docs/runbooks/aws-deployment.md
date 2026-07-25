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
- `ECS_MIGRATION_TASK_DEFINITION` — Terraform output
  `migration_task_definition`
- `FRONTEND_BUCKET` — Terraform output `frontend_bucket`
- `CLOUDFRONT_DISTRIBUTION_ID` — the production distribution ID
- `APPLICATION_URL` — Terraform output `application_url`

Configure one GitHub environment secret:

- `AWS_DEPLOY_ROLE_ARN` — an AWS IAM role trusted through GitHub OIDC and
  attached to the Terraform output `deployment_policy_arn`

Restrict the role trust policy to this repository and the `production`
environment. Do not create long-lived AWS access-key secrets in GitHub.

Before the first task starts, populate the Terraform-created application
secret with the required JSON keys (`DB_APP_USERNAME`, `DB_APP_PASSWORD`,
`JWT_SECRET`, `RATE_LIMIT_KEY_SECRET`, mail settings, and the one-time bootstrap keys). Store values via
the Secrets Manager console or an approved secret-management pipeline; do not
put the JSON in shell history. The migration task uses the RDS-managed master
secret only for schema migration and receives the bootstrap password once.

Set both `domain_name` (frontend/CloudFront) and `api_domain_name` (ALB/API).
The regional ACM certificate covers `api_domain_name`; CloudFront uses that
hostname for its HTTPS backend origin.

## Release order

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
4. Run the migration task and wait for a successful exit before changing the
   service task definition:

```sh
aws ecs run-task \
  --cluster "$ECS_CLUSTER" \
  --task-definition "$MIGRATION_TASK_DEFINITION" \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$PRIVATE_SUBNET_A,$PRIVATE_SUBNET_B],securityGroups=[$ECS_SECURITY_GROUP],assignPublicIp=DISABLED}" \
  --query 'tasks[0].taskArn' --output text
aws ecs wait tasks-stopped --cluster "$ECS_CLUSTER" --tasks "$TASK_ARN"
aws ecs describe-tasks --cluster "$ECS_CLUSTER" --tasks "$TASK_ARN" \
  --query 'tasks[0].containers[0].exitCode'
```

   A non-zero exit code stops the release. Inspect the migration log group,
   fix the migration or data issue, and rerun the task; do not start an
   incompatible application revision.
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
tasks are private and have no public IP. RDS (5432) and Redis (6379) security
groups allow traffic only from the ECS task security group. Check
`/actuator/health`, `/actuator/health/liveness`, and
`/actuator/health/readiness`; no other actuator endpoint should be exposed.
