output "backend_instance_id" {
  value = aws_instance.backend.id
}

output "backend_instance_private_ip" {
  value = aws_instance.backend.private_ip
}

output "backend_instance_public_ip" {
  value = aws_instance.backend.public_ip
}

output "backend_security_group_id" {
  description = "Security group the RDS owner must allow as the TCP/5432 source."
  value       = aws_security_group.backend.id
}

output "backend_instance_role_arn" {
  description = "Role the external runtime secret/KMS policies must authorize."
  value       = aws_iam_role.backend.arn
}

output "load_balancer_dns_name" {
  value = aws_lb.backend.dns_name
}

output "load_balancer_target_group_arn" {
  value = aws_lb_target_group.backend.arn
}

output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "artifact_bucket" {
  value = aws_s3_bucket.artifacts.id
}

output "application_secret_arn" {
  value = aws_secretsmanager_secret.application.arn
}

output "mail_secret_arn" {
  description = "Populate this secret directly in AWS only after SES production access is approved."
  value       = aws_secretsmanager_secret.mail.arn
}

output "external_rds_security_group_id" {
  value = var.rds_security_group_id
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

output "cloudwatch_log_group_name" {
  value = aws_cloudwatch_log_group.runtime.name
}

output "https_load_balancer_url" {
  value = "https://${aws_lb.backend.dns_name}"
}
