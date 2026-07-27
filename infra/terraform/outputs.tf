output "application_url" {
  value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name}"
}

output "frontend_bucket" {
  value = aws_s3_bucket.frontend.id
}

output "uploads_bucket" {
  value = aws_s3_bucket.uploads.id
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.backend.name
}

output "backend_vpc_id" {
  description = "VPC used by the ECS backend."
  value       = local.vpc_id
}

output "backend_subnet_ids" {
  description = "Private subnet IDs used by ECS backend tasks."
  value       = local.backend_subnet_ids
}

output "backend_security_group_id" {
  description = "Security group the external RDS owner must allow as the TCP/5432 source."
  value       = aws_security_group.ecs.id
}

output "ecs_execution_role_arn" {
  description = "Role that the external secret and KMS key policies must authorize."
  value       = aws_iam_role.ecs_execution.arn
}

output "external_rds_security_group_id" {
  description = "Externally managed production RDS security group consumed by this stack."
  value       = local.external_production ? var.production_rds_security_group_id : aws_security_group.database[0].id
}

output "migration_task_definition" {
  value = aws_ecs_task_definition.migration.family
}

output "database_role_task_definition" {
  description = "Staging-only database role task; production database roles are externally managed."
  value       = local.manage_database ? aws_ecs_task_definition.database_role[0].family : null
}

output "application_secret_arn" {
  value     = aws_secretsmanager_secret.application.arn
  sensitive = true
}

output "mail_secret_arn" {
  value     = aws_secretsmanager_secret.mail.arn
  sensitive = true
}

output "deployment_policy_arn" {
  value = aws_iam_policy.deployment.arn
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}
