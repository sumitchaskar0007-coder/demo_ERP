# Production readiness

Production is designed for stateless ECS Fargate tasks. PostgreSQL, Redis/Valkey, and the private
S3 upload bucket are mandatory external services. Container-local uploads and automatic schema
changes are disabled by the production profile.

## Runtime invariants

- Run at least two backend tasks in private application subnets.
- Keep RDS and Redis in isolated data subnets; ports 5432, 6379, and 8081 are never public.
- Set `FLYWAY_ENABLED=false` and `BOOTSTRAP_ENABLED=false` on the ECS service.
- Use the application database role for the ECS service. It needs DML privileges, not schema-owner
  privileges.
- Use the migration database role only in the one-time migration task.
- Set `STORAGE_PROVIDER=s3`; the task role supplies temporary AWS credentials. Do not configure
  static AWS access keys.
- Use `/actuator/health/readiness` for ALB health and `/actuator/health/liveness` for container health.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.

## Secret preparation

Before creating an ECS service task, put one JSON object into the Terraform-created application
secret. It must contain these keys, populated through an approved secret-management channel:

```text
DB_APP_USERNAME, DB_APP_PASSWORD, JWT_SECRET,
MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM_ADDRESS,
SUPER_ADMIN_NAME, SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD
```

Never put secret values in Terraform variable files, shell history, task overrides, logs, or source
control. The initial administrator password is mandatory only for the explicit bootstrap task.

Create the runtime database login once using the RDS master/migration identity and grant only the
required schema usage and table/sequence DML privileges. Revoke schema creation and ownership from
that runtime login.

## One-time Flyway and administrator bootstrap task

Export only non-secret identifiers from Terraform:

```bash
CLUSTER=$(terraform -chdir=infra/terraform output -raw ecs_cluster_name)
TASK_FAMILY=$(terraform -chdir=infra/terraform output -raw migration_task_definition)
```

Run the migration task in the same private subnets and ECS security group as the service. Obtain
the explicit subnet and security-group IDs from the Terraform state/outputs used by your deployment
automation:

```bash
aws ecs run-task \
  --cluster "$CLUSTER" \
  --task-definition "$TASK_FAMILY" \
  --launch-type FARGATE \
  --network-configuration 'awsvpcConfiguration={subnets=[subnet-a,subnet-b],securityGroups=[sg-backend],assignPublicIp=DISABLED}'
```

Capture the returned task ARN, wait, and require an exit code of zero:

```bash
aws ecs wait tasks-stopped --cluster "$CLUSTER" --tasks "$TASK_ARN"
aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK_ARN" \
  --query 'tasks[0].containers[0].{exitCode:exitCode,reason:reason}'
```

The task runs Flyway before the explicit, idempotent administrator bootstrap. Re-running it does
not create a duplicate administrator. A deleted administrator is not recreated by normal ECS task
startup because bootstrap is disabled there.

If migration fails, do not update the ECS service. Preserve the previous task definition, inspect
the migration log stream without exposing secrets or row data, correct the forward migration, and
run a new migration task. Never edit a migration already applied to another environment.

Only after a successful migration task:

```bash
SERVICE=$(terraform -chdir=infra/terraform output -raw ecs_service_name)
aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" \
  --task-definition "$BACKEND_TASK_DEFINITION" --force-new-deployment
aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"
```

Then verify the CloudFront public origin, ALB target health, authenticated upload/download, and an
upload after replacing one ECS task. The object must remain available because it resides in S3.

## Infrastructure deployment

Use a versioned, encrypted S3 Terraform backend with restricted state access. Copy
`infra/terraform/terraform.tfvars.example`, replace identifiers, and use an immutable backend image
tag or digest.

```bash
terraform -chdir=infra/terraform init -backend-config=backend.hcl
terraform -chdir=infra/terraform fmt -check -recursive
terraform -chdir=infra/terraform validate
terraform -chdir=infra/terraform plan -out=tfplan
terraform -chdir=infra/terraform apply tfplan
```

Confirm ACM DNS validation and the alert email subscription. Review sampled WAF requests before
changing the common and SQL-injection managed groups from count mode to blocking.

## Starting capacity

For 20 colleges and 10,000 students, begin with two tasks and measure before changing capacity.
The Terraform default is 1 vCPU/2 GiB per task and a database pool of 20. Total pools across all
tasks must remain safely below the PostgreSQL connection limit.
