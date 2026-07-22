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
  type = string
}

variable "api_domain_name" {
  type        = string
  description = "DNS name used by CloudFront to reach the ALB; its ACM certificate must cover this name"
}

variable "route53_zone_id" {
  type = string
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

variable "desired_count" {
  type    = number
  default = 2

  validation {
    condition     = var.desired_count >= 2
    error_message = "Production ECS service requires at least two tasks"
  }
}
