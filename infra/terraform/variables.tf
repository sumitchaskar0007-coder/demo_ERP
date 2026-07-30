variable "project_name" {
  type    = string
  default = "jadhavr-erp"
}

variable "environment" {
  type    = string
  default = "production"

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be staging or production"
  }
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "domain_name" {
  type        = string
  default     = ""
  description = "Custom frontend domain. Leave empty when temporary_domain is true."
}

variable "api_domain_name" {
  type        = string
  default     = ""
  description = "DNS name used by CloudFront to reach the ALB; its ACM certificate must cover this name"
}

variable "route53_zone_id" {
  type        = string
  default     = ""
  description = "Route 53 hosted zone for custom domains. Leave empty when temporary_domain is true."
}

variable "temporary_domain" {
  type        = bool
  default     = false
  description = "Use the AWS-provided CloudFront domain and an HTTP ALB origin restricted to CloudFront."
}

variable "backend_image" {
  type        = string
  description = "Immutable ECR image URI including a digest or version tag"
}

variable "db_name" {
  type    = string
  default = "college_erp"
}

variable "db_master_username" {
  type    = string
  default = "erp_migration"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.medium"
}

variable "production_vpc_id" {
  type        = string
  default     = ""
  description = "Existing production VPC ID supplied by the platform owner. Required only when environment is production."
}

variable "production_public_subnet_ids" {
  type        = list(string)
  default     = []
  description = "At least two existing production public subnet IDs for the ALB. Required only when environment is production."
}

variable "production_backend_subnet_ids" {
  type        = list(string)
  default     = []
  description = "At least two existing production private subnet IDs for ECS backend tasks. Required only when environment is production."
}

variable "production_cache_subnet_ids" {
  type        = list(string)
  default     = []
  description = "Optional existing production private subnet IDs for Valkey. Defaults to production_backend_subnet_ids."
}

variable "production_rds_endpoint" {
  type        = string
  default     = ""
  description = "Externally managed production RDS endpoint hostname. Required only when environment is production."
}

variable "production_database_port" {
  type        = number
  default     = 5432
  description = "Externally managed production PostgreSQL port."

  validation {
    condition     = var.production_database_port >= 1 && var.production_database_port <= 65535
    error_message = "production_database_port must be between 1 and 65535."
  }
}

variable "production_database_name" {
  type        = string
  default     = ""
  description = "Externally managed production database name. Required only when environment is production."
}

variable "production_rds_security_group_id" {
  type        = string
  default     = ""
  description = "Existing production RDS security-group ID. It is validated but never modified by this stack."
}

variable "production_runtime_database_secret_arn" {
  type        = string
  default     = ""
  description = "Secrets Manager ARN containing runtime database username and password keys."
}

variable "production_migration_database_secret_arn" {
  type        = string
  default     = ""
  description = "Secrets Manager ARN containing migration database username and password keys."
}

variable "production_database_secret_kms_key_arns" {
  type        = list(string)
  default     = []
  description = "Optional customer-managed KMS key ARNs used by the external database secrets."
}

variable "production_database_access_ready" {
  type        = bool
  default     = false
  description = "Set true only after the RDS owner allows port 5432 from the output backend_security_group_id."
}

variable "alert_email" {
  type    = string
  default = ""
}

variable "mail_enabled" {
  type        = bool
  default     = true
  description = "Enable SMTP delivery and require mail credentials in the application secret."
}

variable "github_repository" {
  type        = string
  default     = "sumitchaskar0007-coder/Jadhavr-ERP"
  description = "GitHub owner/repository allowed to assume the deployment role through the production environment."
}

variable "desired_count" {
  type    = number
  default = 2

  validation {
    condition     = var.desired_count >= 0
    error_message = "desired_count cannot be negative"
  }
}

variable "backend_task_cpu" {
  type        = number
  default     = 1024
  description = "Fargate CPU units. Keep 1024 until staging load-test evidence approves 2048."
}

variable "backend_task_memory" {
  type        = number
  default     = 2048
  description = "Fargate memory MiB. Keep 2048 until staging load-test evidence approves 4096."
}

variable "backend_autoscaling_min_capacity" {
  type        = number
  default     = 2
  description = "Minimum backend tasks outside scheduled peak periods."
}

variable "backend_autoscaling_max_capacity" {
  type        = number
  default     = 12
  description = "Maximum backend tasks permitted by target tracking."
}

variable "backend_peak_schedule_enabled" {
  type        = bool
  default     = false
  description = "Enable scheduled pre-scaling only after the institution confirms its login window."
}

variable "backend_peak_scale_out_schedule" {
  type        = string
  default     = "cron(45 7 ? * MON-SAT *)"
  description = "Asia/Kolkata schedule for pre-scaling."
}

variable "backend_peak_scale_in_schedule" {
  type        = string
  default     = "cron(0 10 ? * MON-SAT *)"
  description = "Asia/Kolkata schedule for returning to off-peak capacity."
}

variable "backend_peak_capacity" {
  type        = number
  default     = 8
  description = "Tasks kept ready during the configured login window."
}

variable "backend_requests_per_target" {
  type        = number
  default     = 900
  description = "ALB requests per target per minute target-tracking threshold."
}

variable "backend_db_pool_max_size" {
  type        = number
  default     = 12
  description = "Maximum Hikari connections per backend task."
}

variable "backend_db_pool_min_idle" {
  type        = number
  default     = 2
  description = "Minimum idle Hikari connections per backend task."
}

variable "database_connection_budget" {
  type        = number
  default     = 180
  description = "Maximum aggregate application connections reserved for ECS; verify against the selected RDS class."
}
