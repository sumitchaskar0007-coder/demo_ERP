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

output "migration_task_definition" {
  value = aws_ecs_task_definition.migration.family
}

output "database_role_task_definition" {
  value = aws_ecs_task_definition.database_role.family
}

output "application_secret_arn" {
  value     = aws_secretsmanager_secret.application.arn
  sensitive = true
}

output "deployment_policy_arn" {
  value = aws_iam_policy.deployment.arn
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}
