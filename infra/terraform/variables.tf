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

variable "manage_github_oidc_provider" {
  type        = bool
  default     = true
  description = "Whether this state owns the account-global GitHub OIDC provider. Set false in production when the existing staging state owns it."
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
  description = "Fargate CPU units for the backend task. Keep 1024 until staging load-test evidence justifies 2048."

  validation {
    condition     = contains([256, 512, 1024, 2048, 4096, 8192, 16384], var.backend_task_cpu)
    error_message = "backend_task_cpu must be a supported Fargate CPU value."
  }
}

variable "backend_task_memory" {
  type        = number
  default     = 2048
  description = "Fargate task memory in MiB. Keep 2048 until staging load-test evidence justifies 4096."

  validation {
    condition     = var.backend_task_memory >= 512 && var.backend_task_memory <= 122880
    error_message = "backend_task_memory must be between 512 and 122880 MiB."
  }
}

variable "backend_autoscaling_min_capacity" {
  type        = number
  default     = 2
  description = "Minimum number of backend ECS tasks kept running."

  validation {
    condition     = var.backend_autoscaling_min_capacity >= 2
    error_message = "backend_autoscaling_min_capacity must be at least 2 for availability."
  }
}

variable "backend_autoscaling_max_capacity" {
  type        = number
  default     = 12
  description = "Maximum number of backend ECS tasks created during traffic spikes."

  validation {
    condition = (
      var.backend_autoscaling_max_capacity >= var.backend_autoscaling_min_capacity &&
      var.backend_autoscaling_max_capacity <= 12
    )
    error_message = "backend_autoscaling_max_capacity must be between the minimum capacity and 12."
  }
}

variable "backend_autoscaling_cpu_target" {
  type        = number
  default     = 60
  description = "Average ECS CPU utilization percentage that triggers target tracking."

  validation {
    condition     = var.backend_autoscaling_cpu_target > 0 && var.backend_autoscaling_cpu_target <= 100
    error_message = "backend_autoscaling_cpu_target must be between 1 and 100."
  }
}

variable "backend_autoscaling_memory_target" {
  type        = number
  default     = 70
  description = "Average ECS memory utilization percentage that triggers target tracking."

  validation {
    condition     = var.backend_autoscaling_memory_target > 0 && var.backend_autoscaling_memory_target <= 100
    error_message = "backend_autoscaling_memory_target must be between 1 and 100."
  }
}

variable "backend_requests_per_target" {
  type        = number
  default     = 900
  description = "ALB requests per target per one-minute period for target tracking. Tune only with staging evidence."

  validation {
    condition     = var.backend_requests_per_target >= 1
    error_message = "backend_requests_per_target must be positive."
  }
}

variable "backend_peak_schedule_enabled" {
  type        = bool
  default     = false
  description = "Enable Asia/Kolkata scheduled pre-scaling only after the institution approves its login window and cost."
}

variable "backend_peak_scale_out_schedule" {
  type        = string
  default     = "cron(45 7 ? * MON-SAT *)"
  description = "Asia/Kolkata cron expression for pre-scaling before the expected login period."

  validation {
    condition     = can(regex("^cron\\(.+\\)$", var.backend_peak_scale_out_schedule))
    error_message = "backend_peak_scale_out_schedule must be an EventBridge cron expression."
  }
}

variable "backend_peak_scale_in_schedule" {
  type        = string
  default     = "cron(0 10 ? * MON-SAT *)"
  description = "Asia/Kolkata cron expression for restoring off-peak minimum capacity."

  validation {
    condition     = can(regex("^cron\\(.+\\)$", var.backend_peak_scale_in_schedule))
    error_message = "backend_peak_scale_in_schedule must be an EventBridge cron expression."
  }
}

variable "backend_peak_capacity" {
  type        = number
  default     = 8
  description = "Minimum tasks kept ready during an approved peak login period."

  validation {
    condition     = var.backend_peak_capacity >= 2 && var.backend_peak_capacity <= 12
    error_message = "backend_peak_capacity must be between 2 and 12."
  }
}

variable "backend_db_pool_max_size" {
  type        = number
  default     = 12
  description = "Maximum Hikari connections per backend task; validate against the external RDS owner's connection budget."

  validation {
    condition     = var.backend_db_pool_max_size >= 1 && var.backend_db_pool_max_size <= 50
    error_message = "backend_db_pool_max_size must be between 1 and 50."
  }
}

variable "backend_db_pool_min_idle" {
  type        = number
  default     = 2
  description = "Minimum idle Hikari connections per backend task."

  validation {
    condition     = var.backend_db_pool_min_idle >= 0
    error_message = "backend_db_pool_min_idle cannot be negative."
  }
}

variable "database_connection_budget" {
  type        = number
  default     = 180
  description = "Maximum aggregate application connections approved for ECS; production must be confirmed by the external RDS owner."

  validation {
    condition     = var.database_connection_budget >= 1
    error_message = "database_connection_budget must be positive."
  }
}

variable "waf_public_auth_edge_limit" {
  type        = number
  default     = 10000
  description = "Coarse per-source-IP auth request ceiling per five minutes. Redis account/user limits remain the primary control."

  validation {
    condition     = var.waf_public_auth_edge_limit >= 1000 && var.waf_public_auth_edge_limit <= 2000000
    error_message = "waf_public_auth_edge_limit must be between 1000 and 2000000 requests per five minutes."
  }
}

variable "alb_latency_p95_alarm_seconds" {
  type        = number
  default     = 1
  description = "ALB target p95 latency alarm threshold in seconds."

  validation {
    condition     = var.alb_latency_p95_alarm_seconds > 0
    error_message = "alb_latency_p95_alarm_seconds must be positive."
  }
}

variable "alb_latency_p99_alarm_seconds" {
  type        = number
  default     = 2
  description = "ALB target p99 latency alarm threshold in seconds."

  validation {
    condition     = var.alb_latency_p99_alarm_seconds > 0
    error_message = "alb_latency_p99_alarm_seconds must be positive."
  }
}

variable "async_queues_enabled" {
  type        = bool
  default     = false
  description = "Create email/report queues and a separate worker service after staging cost and deployment approval."
}

variable "async_worker_desired_count" {
  type        = number
  default     = 1
  description = "Background worker task count when async queues are enabled."

  validation {
    condition     = var.async_worker_desired_count >= 1 && var.async_worker_desired_count <= 4
    error_message = "async_worker_desired_count must be between 1 and 4."
  }
}

variable "async_worker_task_cpu" {
  type        = number
  default     = 512
  description = "Fargate CPU units for the combined email/report worker."
}

variable "async_worker_task_memory" {
  type        = number
  default     = 1024
  description = "Fargate memory in MiB for the combined email/report worker."
}

variable "async_worker_db_pool_max_size" {
  type        = number
  default     = 4
  description = "Maximum Hikari connections per background worker task."

  validation {
    condition     = var.async_worker_db_pool_max_size >= 1 && var.async_worker_db_pool_max_size <= 20
    error_message = "async_worker_db_pool_max_size must be between 1 and 20."
  }
}

variable "async_worker_db_pool_min_idle" {
  type        = number
  default     = 1
  description = "Minimum idle Hikari connections per background worker task."

  validation {
    condition     = var.async_worker_db_pool_min_idle >= 0
    error_message = "async_worker_db_pool_min_idle cannot be negative."
  }
}

variable "malware_protection_enabled" {
  type        = bool
  default     = true
  description = "Enable GuardDuty Malware Protection and managed scan-result tags for private uploads."
}

variable "email_queue_depth_alarm_threshold" {
  type        = number
  default     = 100
  description = "Visible email messages that trigger the queue-depth alarm."

  validation {
    condition     = var.email_queue_depth_alarm_threshold >= 1
    error_message = "email_queue_depth_alarm_threshold must be at least 1."
  }
}

variable "report_queue_depth_alarm_threshold" {
  type        = number
  default     = 50
  description = "Visible report messages that trigger the queue-depth alarm."

  validation {
    condition     = var.report_queue_depth_alarm_threshold >= 1
    error_message = "report_queue_depth_alarm_threshold must be at least 1."
  }
}

variable "email_queue_oldest_alarm_seconds" {
  type        = number
  default     = 300
  description = "Age in seconds that triggers the email queue oldest-message alarm."

  validation {
    condition     = var.email_queue_oldest_alarm_seconds >= 1
    error_message = "email_queue_oldest_alarm_seconds must be at least 1."
  }
}

variable "report_queue_oldest_alarm_seconds" {
  type        = number
  default     = 900
  description = "Age in seconds that triggers the report queue oldest-message alarm."

  validation {
    condition     = var.report_queue_oldest_alarm_seconds >= 1
    error_message = "report_queue_oldest_alarm_seconds must be at least 1."
  }
}
