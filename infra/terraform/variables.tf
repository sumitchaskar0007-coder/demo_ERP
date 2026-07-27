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
