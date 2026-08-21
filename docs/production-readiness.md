# Production readiness

Production is designed for stateless ECS Fargate tasks in the externally managed production VPC.
The production VPC and all production RDS infrastructure are owned outside this Terraform stack.
Redis/Valkey and the private S3 upload bucket remain application-stack resources. Container-local
uploads and automatic schema changes are disabled by the production profile.

## Runtime invariants

- Run at least two backend tasks in private application subnets.
- Keep RDS and Redis in isolated data subnets; ports 5432, 6379, and 8081 are never public.
- Set `FLYWAY_ENABLED=false` and `BOOTSTRAP_ENABLED=false` on the ECS service.
- Use the application database role for the ECS service. It needs DML privileges, not schema-owner
  privileges.
- The RDS owner controls the migration identity and Flyway execution. The application deployment
  workflow never runs production migrations.
- Set `STORAGE_PROVIDER=s3`; the task role supplies temporary AWS credentials. Do not configure
  static AWS access keys.
- Use `/actuator/health/readiness` for ALB health and `/actuator/health/liveness` for container health.
- Keep `spring.jpa.hibernate.ddl-auto=validate`.

## Secret preparation

Supply separate externally managed Secrets Manager ARNs for the production runtime and migration
database identities. Each database secret must contain string keys named `username` and `password`.
The application stack reads those secrets but does not create, rotate, or modify them.

Before creating an ECS service task, populate the Terraform-created application secret with the
remaining application keys through an approved secret-management channel:

```text
JWT_SECRET, RATE_LIMIT_KEY_SECRET
```

Terraform stores generated SMTP credentials in a separate mail secret.
Never put secret values in Terraform variable files, shell history, task overrides, logs, or source
control.

Set `api_domain_name` to a DNS name whose ACM certificate is attached to the ALB. CloudFront uses
that hostname as its HTTPS backend origin; do not use the raw `*.elb.amazonaws.com` hostname with a
certificate issued only for the application domain.

The production RDS owner creates and manages the runtime and migration database roles. The runtime
identity needs schema usage and table/sequence DML privileges but no schema creation or ownership.

## Database-owner Flyway handoff

Terraform defines a migration task wired to the externally supplied migration
secret as an optional execution vehicle for the database owner. Production sets
`BOOTSTRAP_ENABLED=false` in that task, and the application GitHub workflow
does not register or run a migration revision.

The database owner may use their own approved Flyway process or explicitly
invoke the Terraform task. Export only non-secret identifiers:

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

Give the RDS owner the `backend_security_group_id` Terraform output. The RDS security group must
allow TCP/5432 from exactly that security group; this application stack never modifies the
externally managed RDS security group.

Capture the returned task ARN, wait, and require an exit code of zero:

```bash
aws ecs wait tasks-stopped --cluster "$CLUSTER" --tasks "$TASK_ARN"
aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK_ARN" \
  --query 'tasks[0].containers[0].{exitCode:exitCode,reason:reason}'
```

The production task runs Flyway only. Initial application-administrator
provisioning must use an independently approved application process; it is not
coupled to database-owner migrations.

If migration fails, do not update the ECS service. Preserve the previous task definition, inspect
the migration log stream without exposing secrets or row data, correct the forward migration, and
run a new migration task. Never edit a migration already applied to another environment.

Only after the database owner confirms a successful migration:

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
`infra/terraform/production.external.tfvars.example`, replace identifiers, and use an immutable
backend image tag or digest. Production values must include the existing VPC/subnets, RDS endpoint,
database port and name, RDS security-group ID, and both database secret ARNs.

The current historical backend key contains the live staging state despite its
name. Production must use a brand-new backend key/state. Never change
`environment` from `staging` to `production` in the existing state. Use
`backend.production.hcl` and a separate `.terraform-production` data directory
for every production command.

For `environment = "production"`, this stack creates no VPC, NAT gateway, route table, RDS instance,
DB subnet group, RDS parameter group, RDS KMS key, RDS monitoring role, or RDS alarm. Review the plan
and stop if any of those production resources appear.

Use a two-phase network handoff:

1. Apply with `temporary_domain=true`, `production_database_access_ready=false`, and
   `desired_count=0`.
2. Give outputs `backend_security_group_id` and `ecs_execution_role_arn` to the RDS owner.
3. The owner allows TCP/5432 from that security group, authorizes the external
   secrets/KMS keys, and completes Flyway.
4. Set `production_database_access_ready=true` and `desired_count=2` or higher,
   then apply again.

The live staging state currently owns the `collegeerp.example` DNS records. Keep
phase one on the AWS temporary domain. A later custom-domain cutover requires
an explicit DNS/state handoff; do not let the new production state overwrite
records still owned by staging.

```bash
cd infra/terraform
cp backend.production.hcl.example backend.production.hcl
cp production.external.tfvars.example production.external.tfvars
# Replace every example endpoint, secret ARN, domain, image, and alert address.
TF_DATA_DIR=.terraform-production terraform init -reconfigure -backend-config=backend.production.hcl
TF_DATA_DIR=.terraform-production terraform fmt -check -recursive
TF_DATA_DIR=.terraform-production terraform validate
TF_DATA_DIR=.terraform-production terraform plan -var-file=production.external.tfvars -out=production.tfplan
# Apply only an approved production plan:
TF_DATA_DIR=.terraform-production terraform apply production.tfplan
```

Confirm ACM DNS validation and the alert email subscription. Review sampled WAF requests before
changing the common and SQL-injection managed groups from count mode to blocking.

## Starting capacity

For 20 colleges and 10,000 students, begin with two tasks and measure before changing capacity.
The Terraform default is 1 vCPU/2 GiB per task and a database pool of 20. Total pools across all
tasks must remain safely below the PostgreSQL connection limit.
