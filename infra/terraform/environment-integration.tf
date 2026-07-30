locals {
  external_production = var.environment == "production"
  manage_network      = !local.external_production
  manage_database     = !local.external_production

  production_cache_subnet_ids = length(var.production_cache_subnet_ids) > 0 ? var.production_cache_subnet_ids : var.production_backend_subnet_ids

  vpc_id               = local.manage_network ? aws_vpc.main[0].id : var.production_vpc_id
  vpc_cidr             = local.manage_network ? aws_vpc.main[0].cidr_block : data.aws_vpc.production[0].cidr_block
  public_subnet_ids    = local.manage_network ? aws_subnet.public[*].id : var.production_public_subnet_ids
  backend_subnet_ids   = local.manage_network ? aws_subnet.application[*].id : var.production_backend_subnet_ids
  cache_subnet_ids     = local.manage_network ? aws_subnet.data[*].id : local.production_cache_subnet_ids
  database_endpoint    = local.manage_database ? aws_db_instance.postgres[0].address : var.production_rds_endpoint
  database_port        = local.manage_database ? aws_db_instance.postgres[0].port : var.production_database_port
  database_name        = local.manage_database ? var.db_name : var.production_database_name
  runtime_secret_arn   = local.manage_database ? aws_secretsmanager_secret.application.arn : var.production_runtime_database_secret_arn
  migration_secret_arn = local.manage_database ? aws_db_instance.postgres[0].master_user_secret[0].secret_arn : var.production_migration_database_secret_arn
}

data "aws_vpc" "production" {
  count = local.external_production && var.production_vpc_id != "" ? 1 : 0
  id    = var.production_vpc_id
}

data "aws_subnet" "production_public" {
  for_each = local.external_production ? toset(var.production_public_subnet_ids) : toset([])
  id       = each.value
}

data "aws_subnet" "production_backend" {
  for_each = local.external_production ? toset(var.production_backend_subnet_ids) : toset([])
  id       = each.value
}

data "aws_subnet" "production_cache" {
  for_each = local.external_production ? toset(local.production_cache_subnet_ids) : toset([])
  id       = each.value
}

data "aws_security_group" "production_rds" {
  count = local.external_production && var.production_rds_security_group_id != "" ? 1 : 0
  id    = var.production_rds_security_group_id
}

resource "terraform_data" "production_contract" {
  count = local.external_production ? 1 : 0

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
