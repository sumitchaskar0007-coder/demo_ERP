variable "project_name" {
  type    = string
  default = "jadhavr-erp"
}

variable "environment" {
  type    = string
  default = "production"

  validation {
    condition     = var.environment == "production"
    error_message = "This isolated stack is production-only."
  }
}

variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "vpc_id" {
  type        = string
  description = "Existing production VPC ID."
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Two existing public subnet IDs for the ALB."

  validation {
    condition     = length(var.public_subnet_ids) >= 2
    error_message = "At least two public subnets are required."
  }
}

variable "instance_subnet_id" {
  type        = string
  description = "Existing production public subnet used by the EC2 instance. A public IP is required because the external VPC has no NAT or service endpoints."
}

variable "rds_security_group_id" {
  type        = string
  default     = ""
  description = "Externally managed production RDS security-group ID. Leave empty during phase one; this stack validates but never modifies it."

  validation {
    condition     = var.rds_security_group_id == "" || can(regex("^sg-[0-9a-f]+$", var.rds_security_group_id))
    error_message = "rds_security_group_id must be empty for phase one or an EC2 security-group ID."
  }
}

variable "runtime_database_secret_arn" {
  type        = string
  default     = ""
  description = "Externally managed runtime database secret ARN. Leave empty during phase one."
}

variable "runtime_database_secret_kms_key_arns" {
  type        = list(string)
  default     = []
  description = "Optional CMK ARNs used by the external runtime database secret."
}

variable "documents_bucket_name" {
  type        = string
  description = "Existing private production documents/uploads bucket."
}

variable "documents_kms_key_arn" {
  type        = string
  description = "KMS key ARN used by the existing production documents bucket."
}

variable "instance_type" {
  type        = string
  default     = "t3.medium"
  description = "EC2 instance type for the backend."
}

variable "root_volume_size_gib" {
  type    = number
  default = 30

  validation {
    condition     = var.root_volume_size_gib >= 20
    error_message = "The backend root volume must be at least 20 GiB."
  }
}

variable "frontend_origins" {
  type        = list(string)
  description = "Exact HTTPS frontend origins allowed by CORS."

  validation {
    condition = alltrue([
      for origin in var.frontend_origins : startswith(origin, "https://")
    ])
    error_message = "Every frontend origin must use HTTPS."
  }
}

variable "api_certificate_arn" {
  type        = string
  description = "Existing issued ACM certificate ARN for api.jadhavaredu.com in aws_region."

  validation {
    condition     = can(regex("^arn:aws[a-z-]*:acm:[a-z0-9-]+:[0-9]{12}:certificate/", var.api_certificate_arn))
    error_message = "api_certificate_arn must be an ACM certificate ARN."
  }
}

variable "github_repository" {
  type        = string
  description = "GitHub owner/repository trusted to deploy from the protected production environment."

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "github_repository must use owner/repository format."
  }
}

variable "frontend_bucket_name" {
  type        = string
  description = "Existing private bucket currently serving the production frontend."
}

variable "cloudfront_distribution_id" {
  type        = string
  description = "Existing CloudFront distribution serving the production domain."
}
