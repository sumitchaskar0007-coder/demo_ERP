output "application_url" {
  value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name}"
}

output "frontend_bucket" {
  value = aws_s3_bucket.frontend.id
}

output "uploads_bucket" {
  value = aws_s3_bucket.uploads.id
}

output "uploads_kms_key_arn" {
  description = "Customer-managed KMS key for private upload objects; unrelated to the external RDS keys."
  value       = aws_kms_key.uploads.arn
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

output "ecs_async_worker_service_name" {
  description = "Dedicated email/report worker service when async_queues_enabled=true."
  value       = try(aws_ecs_service.async_worker[0].name, null)
}

output "ecs_async_worker_task_role_arn" {
  description = "Least-privilege SQS/S3 task role used by the dedicated async worker."
  value       = try(aws_iam_role.async_worker[0].arn, null)
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

output "email_queue_url" {
  description = "Email queue URL when async_queues_enabled=true; null until application producers/workers are approved."
  value       = try(aws_sqs_queue.email[0].url, null)
}

output "report_queue_url" {
  description = "Report queue URL when async_queues_enabled=true; null until application producers/workers are approved."
  value       = try(aws_sqs_queue.report[0].url, null)
}

output "capacity_dashboard_name" {
  description = "CloudWatch dashboard for ECS, ALB, Valkey, and application pressure signals."
  value       = aws_cloudwatch_dashboard.capacity.dashboard_name
}

output "alerts_topic_arn" {
  description = "Encrypted SNS topic used by the application capacity alarms."
  value       = aws_sns_topic.alerts.arn
}

output "malware_protection_plan_id" {
  description = "GuardDuty upload malware-protection plan ID when explicitly enabled."
  value       = try(aws_guardduty_malware_protection_plan.uploads[0].id, null)
}
