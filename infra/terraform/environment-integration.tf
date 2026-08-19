locals {
  external_production = var.environment == "production"
  # Every environment is isolated and owned by its own Terraform state. In
  # particular, production must never reuse the preproduction VPC or RDS.
  manage_network  = true
  manage_database = true

  production_cache_subnet_ids = length(var.production_cache_subnet_ids) > 0 ? var.production_cache_subnet_ids : var.production_backend_subnet_ids

  vpc_id               = aws_vpc.main[0].id
  vpc_cidr             = aws_vpc.main[0].cidr_block
  public_subnet_ids    = aws_subnet.public[*].id
  backend_subnet_ids   = aws_subnet.application[*].id
  cache_subnet_ids     = aws_subnet.data[*].id
  database_endpoint    = aws_db_instance.postgres[0].address
  database_port        = aws_db_instance.postgres[0].port
  database_name        = var.db_name
  runtime_secret_arn   = aws_secretsmanager_secret.application.arn
  migration_secret_arn = aws_db_instance.postgres[0].master_user_secret[0].secret_arn
}

resource "terraform_data" "deployment_environment_contract" {
  input = {
    environment          = var.environment
    api_desired_count    = var.desired_count
    async_queues_enabled = var.async_queues_enabled
    worker_desired_count = var.async_worker_desired_count
    mail_enabled         = var.mail_enabled
  }

  lifecycle {
    precondition {
      condition     = var.environment != "production" || var.db_multi_az
      error_message = "Production RDS must remain Multi-AZ. A reduction in database redundancy requires a separate architecture and outage review."
    }
    precondition {
      condition     = var.environment != "production" || var.cache_cluster_count == 2
      error_message = "Production Valkey must retain two nodes so a node replacement or failure does not remove the cache service."
    }
    precondition {
      condition     = var.legacy_cache_enabled || var.green_enabled
      error_message = "Disabling the legacy cache requires the Green serverless Valkey architecture to be enabled."
    }
    precondition {
      condition     = var.container_insights_mode != "disabled" || (var.green_cutover_enabled && var.green_worker_active)
      error_message = "Disabling Container Insights requires active Green API and worker ALB health targets so task health remains monitored."
    }
    precondition {
      condition     = var.environment != "production" || var.desired_count == 0 || var.desired_count >= 2
      error_message = "Production must use zero API tasks only during bootstrap, or at least two API tasks during service."
    }
    precondition {
      condition = var.environment != "preprod" || (
        var.desired_count >= 2 &&
        !var.temporary_domain &&
        var.domain_name != "" &&
        var.route53_zone_id != "" &&
        var.async_queues_enabled &&
        var.async_worker_desired_count >= 1 &&
        var.mail_enabled
      )
      error_message = "Preproduction must have its own DNS domain and run at least two API tasks plus the async worker, SQS email/report queues, and SES mail delivery."
    }
  }
}

data "aws_vpc" "production" {
  count = 0
  id    = var.production_vpc_id
}

data "aws_subnet" "production_public" {
  for_each = toset([])
  id       = each.value
}

data "aws_subnet" "production_backend" {
  for_each = toset([])
  id       = each.value
}

data "aws_subnet" "production_cache" {
  for_each = toset([])
  id       = each.value
}

data "aws_security_group" "production_rds" {
  count = 0
  id    = var.production_rds_security_group_id
}

resource "terraform_data" "production_contract" {
  count = 0

  input = {
    vpc_id                        = var.production_vpc_id
    public_subnet_ids             = var.production_public_subnet_ids
    backend_subnet_ids            = var.production_backend_subnet_ids
    cache_subnet_ids              = local.production_cache_subnet_ids
    rds_endpoint                  = var.production_rds_endpoint
    database_port                 = var.production_database_port
    database_name                 = var.production_database_name
    rds_security_group_id         = var.production_rds_security_group_id
    runtime_database_secret_arn   = var.production_runtime_database_secret_arn
    migration_database_secret_arn = var.production_migration_database_secret_arn
    database_secret_kms_key_arns  = var.production_database_secret_kms_key_arns
    database_access_ready         = var.production_database_access_ready
  }

  lifecycle {
    precondition {
      condition     = can(regex("^vpc-[0-9a-f]+$", var.production_vpc_id))
      error_message = "production_vpc_id must be an existing VPC ID."
    }
    precondition {
      condition = (
        length(var.production_public_subnet_ids) >= 2 &&
        length(toset(var.production_public_subnet_ids)) == length(var.production_public_subnet_ids)
      )
      error_message = "Production requires at least two existing public ALB subnets."
    }
    precondition {
      condition = (
        length(var.production_backend_subnet_ids) >= 2 &&
        length(toset(var.production_backend_subnet_ids)) == length(var.production_backend_subnet_ids)
      )
      error_message = "Production requires at least two existing private ECS backend subnets."
    }
    precondition {
      condition     = length(local.production_cache_subnet_ids) >= 2
      error_message = "Production requires at least two existing private Valkey subnets."
    }
    precondition {
      condition     = can(regex("^[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]$", var.production_rds_endpoint))
      error_message = "production_rds_endpoint must be a hostname without a URL scheme."
    }
    precondition {
      condition     = var.production_database_port == 5432
      error_message = "The production RDS handoff requires PostgreSQL port 5432."
    }
    precondition {
      condition     = can(regex("^[A-Za-z_][A-Za-z0-9_]*$", var.production_database_name))
      error_message = "production_database_name must be a valid PostgreSQL identifier."
    }
    precondition {
      condition     = can(regex("^sg-[0-9a-f]+$", var.production_rds_security_group_id))
      error_message = "production_rds_security_group_id must be an existing security-group ID."
    }
    precondition {
      condition     = can(regex("^arn:aws[a-z-]*:secretsmanager:", var.production_runtime_database_secret_arn))
      error_message = "production_runtime_database_secret_arn must be a Secrets Manager ARN."
    }
    precondition {
      condition     = can(regex("^arn:aws[a-z-]*:secretsmanager:", var.production_migration_database_secret_arn))
      error_message = "production_migration_database_secret_arn must be a Secrets Manager ARN."
    }
    precondition {
      condition     = var.production_runtime_database_secret_arn != var.production_migration_database_secret_arn
      error_message = "Runtime and migration database secrets must be separate."
    }
    precondition {
      condition = alltrue([
        for arn in var.production_database_secret_kms_key_arns :
        can(regex("^arn:aws[a-z-]*:kms:", arn))
      ])
      error_message = "Every production_database_secret_kms_key_arns value must be a KMS key ARN."
    }
    precondition {
      condition = try(
        data.aws_security_group.production_rds[0].vpc_id == var.production_vpc_id,
        false
      )
      error_message = "The production RDS security group must belong to production_vpc_id."
    }
    precondition {
      condition = alltrue(concat(
        [for subnet in data.aws_subnet.production_public : subnet.vpc_id == var.production_vpc_id],
        [for subnet in data.aws_subnet.production_backend : subnet.vpc_id == var.production_vpc_id],
        [for subnet in data.aws_subnet.production_cache : subnet.vpc_id == var.production_vpc_id]
      ))
      error_message = "All supplied production subnets must belong to production_vpc_id."
    }
    precondition {
      condition = length(setintersection(
        toset(var.production_public_subnet_ids),
        toset(var.production_backend_subnet_ids)
      )) == 0
      error_message = "Production public and backend subnet sets must be disjoint."
    }
    precondition {
      condition = try(
        data.aws_vpc.production[0].enable_dns_support &&
        data.aws_vpc.production[0].enable_dns_hostnames,
        false
      )
      error_message = "The production VPC must have DNS support and DNS hostnames enabled."
    }
    precondition {
      condition = (
        length(toset([for subnet in data.aws_subnet.production_public : subnet.availability_zone])) >= 2 &&
        length(toset([for subnet in data.aws_subnet.production_backend : subnet.availability_zone])) >= 2 &&
        length(toset([for subnet in data.aws_subnet.production_cache : subnet.availability_zone])) >= 2
      )
      error_message = "Production public, backend, and cache subnets must each span at least two Availability Zones."
    }
  }
}
