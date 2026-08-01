data "aws_caller_identity" "current" {}

data "aws_iam_openid_connect_provider" "github" {
  arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

data "aws_vpc" "production" {
  id = var.vpc_id
}

data "aws_subnet" "public" {
  for_each = toset(var.public_subnet_ids)
  id       = each.value
}

data "aws_subnet" "instance" {
  id = var.instance_subnet_id
}

data "aws_security_group" "rds" {
  count = var.rds_security_group_id == "" ? 0 : 1
  id    = var.rds_security_group_id
}

data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "terraform_data" "external_contract" {
  input = {
    vpc_id                      = var.vpc_id
    public_subnet_ids           = var.public_subnet_ids
    instance_subnet_id          = var.instance_subnet_id
    rds_security_group_id       = var.rds_security_group_id
    runtime_database_secret_arn = var.runtime_database_secret_arn
    documents_bucket_name       = var.documents_bucket_name
    documents_kms_key_arn       = var.documents_kms_key_arn
  }

  lifecycle {
    precondition {
      condition = (
        var.rds_security_group_id == "" ||
        try(data.aws_security_group.rds[0].vpc_id == var.vpc_id, false)
      )
      error_message = "The external RDS security group must belong to vpc_id."
    }

    precondition {
      condition = alltrue(concat(
        [for subnet in data.aws_subnet.public : subnet.vpc_id == var.vpc_id],
        [data.aws_subnet.instance.vpc_id == var.vpc_id]
      ))
      error_message = "Every supplied subnet must belong to vpc_id."
    }

    precondition {
      condition     = length(toset([for subnet in data.aws_subnet.public : subnet.availability_zone])) >= 2
      error_message = "The ALB public subnets must span at least two Availability Zones."
    }

    precondition {
      condition = (
        var.runtime_database_secret_arn == "" ||
        can(regex("^arn:aws[a-z-]*:secretsmanager:", var.runtime_database_secret_arn))
      )
      error_message = "runtime_database_secret_arn must be empty for phase one or a Secrets Manager ARN."
    }
  }
}

resource "aws_security_group" "alb" {
  name = "${local.name}-alb"
  # Preserve the original description because AWS treats it as immutable and
  # replacing this live group would unnecessarily interrupt the ALB.
  description = "Temporary public HTTP entry point for the production EC2 backend"
  vpc_id      = var.vpc_id

  # Public HTTP is required only to redirect the API hostname to HTTPS.
  #tfsec:ignore:aws-ec2-no-public-ingress-sgr
  ingress {
    description = "Redirect public HTTP requests to HTTPS"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # The ALB is the intentionally public TLS edge for api.jadhavaredu.com.
  #tfsec:ignore:aws-ec2-no-public-ingress-sgr
  ingress {
    description = "HTTPS origin traffic"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Forward API traffic to backend targets"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.production.cidr_block]
  }
}

resource "aws_security_group" "backend" {
  name        = "${local.name}-backend"
  description = "EC2 backend accepts application traffic only from the ALB"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Application traffic from the ALB only"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # The external VPC has no NAT gateway or VPC endpoints. HTTPS egress is
  # required for SSM, ECR, S3, Secrets Manager, and approved image pulls.
  #tfsec:ignore:aws-ec2-no-public-egress-sgr
  egress {
    description = "AWS APIs and approved HTTPS registries"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "egress" {
    for_each = var.rds_security_group_id == "" ? [] : [var.rds_security_group_id]

    content {
      description     = "PostgreSQL to the external production RDS group only"
      from_port       = 5432
      to_port         = 5432
      protocol        = "tcp"
      security_groups = [egress.value]
    }
  }
}

# The API must be internet-facing; Route 53 points api.jadhavaredu.com here.
#tfsec:ignore:aws-elb-alb-not-public
resource "aws_lb" "backend" {
  name                       = substr(local.name, 0, 32)
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = var.public_subnet_ids
  drop_invalid_header_fields = true
  enable_deletion_protection = true
}

resource "aws_lb_target_group" "backend" {
  name        = substr("${local.name}-api", 0, 32)
  port        = 8081
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = var.vpc_id

  deregistration_delay = 30

  health_check {
    enabled             = true
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.api_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# Release images contain no secret values. AWS-managed AES-256 encryption is
# intentional; access is restricted by repository IAM permissions.
#tfsec:ignore:aws-ecr-repository-customer-key
resource "aws_ecr_repository" "backend" {
  name                 = "${var.project_name}-${var.environment}-backend"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

# This private bucket contains non-secret release scripts/config identifiers.
# Object access is audited through IAM/CloudTrail; a separate logging bucket
# would add recursive log-management overhead for no sensitive payload.
#tfsec:ignore:aws-s3-enable-bucket-logging
resource "aws_s3_bucket" "artifacts" {
  bucket = "${var.project_name}-${var.environment}-artifacts-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket                  = aws_s3_bucket.artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Release artifacts contain no credentials, so SSE-S3 is sufficient here.
#tfsec:ignore:aws-s3-encryption-customer-key
resource "aws_s3_bucket_server_side_encryption_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  rule {
    id     = "expire-old-artifacts"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }

  depends_on = [aws_s3_bucket_versioning.artifacts]
}

# Runtime logs use the account-managed CloudWatch encryption key. The group
# contains application/Redis logs, never database or application secret values.
#tfsec:ignore:aws-cloudwatch-log-group-customer-key
resource "aws_cloudwatch_log_group" "runtime" {
  name              = "/ec2/${local.name}"
  retention_in_days = 30
}

# The secret uses the account-scoped AWS managed Secrets Manager key. RDS
# secret KMS keys remain externally owned and are accepted only as inputs.
#tfsec:ignore:aws-ssm-secret-use-customer-key
resource "aws_secretsmanager_secret" "application" {
  name                    = "${var.project_name}-${var.environment}/ec2-application"
  recovery_window_in_days = 30

  lifecycle {
    prevent_destroy = true
  }
}

# SMTP credentials are populated directly in Secrets Manager after SES
# production access is approved; Terraform manages only the empty container.
#tfsec:ignore:aws-ssm-secret-use-customer-key
resource "aws_secretsmanager_secret" "mail" {
  name                    = "${var.project_name}-${var.environment}/ec2-mail"
  recovery_window_in_days = 30

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_iam_role" "backend" {
  name = "${local.name}-instance"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.backend.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "backend" {
  name = "backend-runtime-and-deployment"
  role = aws_iam_role.backend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Effect = "Allow"
          Action = [
            "ecr:BatchGetImage",
            "ecr:GetDownloadUrlForLayer"
          ]
          Resource = aws_ecr_repository.backend.arn
        },
        {
          Effect   = "Allow"
          Action   = ["ecr:GetAuthorizationToken"]
          Resource = "*"
        },
        {
          Effect   = "Allow"
          Action   = ["s3:GetObject"]
          Resource = "${aws_s3_bucket.artifacts.arn}/*"
        },
        {
          Effect = "Allow"
          Action = [
            "s3:GetObject",
            "s3:GetObjectTagging",
            "s3:PutObject",
            "s3:DeleteObject"
          ]
          Resource = "arn:aws:s3:::${var.documents_bucket_name}/*"
        },
        {
          Effect = "Allow"
          Action = [
            "s3:GetBucketLocation",
            "s3:ListBucket"
          ]
          Resource = [
            aws_s3_bucket.artifacts.arn,
            "arn:aws:s3:::${var.documents_bucket_name}"
          ]
        },
        {
          Effect = "Allow"
          Action = ["secretsmanager:GetSecretValue"]
          Resource = concat(
            [
              aws_secretsmanager_secret.application.arn,
              aws_secretsmanager_secret.mail.arn
            ],
            var.runtime_database_secret_arn == "" ? [] : [var.runtime_database_secret_arn]
          )
        },
        {
          Effect = "Allow"
          Action = [
            "kms:Decrypt",
            "kms:Encrypt",
            "kms:GenerateDataKey"
          ]
          Resource = var.documents_kms_key_arn
        },
        {
          Effect = "Allow"
          Action = [
            "logs:CreateLogStream",
            "logs:DescribeLogStreams",
            "logs:PutLogEvents"
          ]
          Resource = "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:log-group:${aws_cloudwatch_log_group.runtime.name}:*"
        }
      ],
      length(var.runtime_database_secret_kms_key_arns) == 0 ? [] : [
        {
          Effect   = "Allow"
          Action   = ["kms:Decrypt"]
          Resource = var.runtime_database_secret_kms_key_arns
        }
      ]
    )
  })
}

resource "aws_iam_instance_profile" "backend" {
  name = "${local.name}-instance"
  role = aws_iam_role.backend.name
}

resource "aws_iam_role" "github_deploy" {
  name = "${local.name}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = data.aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repository}:environment:production"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_deploy" {
  name = "immutable-ec2-release-deployment"
  role = aws_iam_role.github_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:BatchGetImage",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeImages",
          "ecr:GetDownloadUrlForLayer",
          "ecr:InitiateLayerUpload",
          "ecr:PutImage",
          "ecr:UploadLayerPart"
        ]
        Resource = aws_ecr_repository.backend.arn
      },
      {
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = [
          "${aws_s3_bucket.artifacts.arn}/*",
          "arn:aws:s3:::${var.frontend_bucket_name}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetBucketLocation",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "arn:aws:s3:::${var.frontend_bucket_name}"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:DeleteObject"
        ]
        Resource = "arn:aws:s3:::${var.frontend_bucket_name}/*"
      },
      {
        Effect = "Allow"
        Action = ["ssm:SendCommand"]
        Resource = [
          "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${aws_instance.backend.id}",
          "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetCommandInvocation",
          "ssm:ListCommandInvocations"
        ]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["elasticloadbalancing:DescribeTargetHealth"]
        Resource = aws_lb_target_group.backend.arn
      },
      {
        Effect = "Allow"
        Action = [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation"
        ]
        Resource = "arn:aws:cloudfront::${data.aws_caller_identity.current.account_id}:distribution/${var.cloudfront_distribution_id}"
      }
    ]
  })
}

resource "aws_instance" "backend" {
  ami                         = data.aws_ssm_parameter.al2023_ami.value
  instance_type               = var.instance_type
  subnet_id                   = var.instance_subnet_id
  associate_public_ip_address = true
  disable_api_termination     = true
  vpc_security_group_ids      = [aws_security_group.backend.id]
  iam_instance_profile        = aws_iam_instance_profile.backend.name
  monitoring                  = true
  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/templates/bootstrap.sh.tftpl", {
    project_name = var.project_name
  })

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "enabled"
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size_gib
    encrypted             = true
    delete_on_termination = true
  }

  tags = {
    Name = "${local.name}-backend"
  }

  depends_on = [
    aws_iam_role_policy_attachment.ssm,
    aws_iam_role_policy.backend,
    terraform_data.external_contract
  ]
}

resource "aws_lb_target_group_attachment" "backend" {
  target_group_arn = aws_lb_target_group.backend.arn
  target_id        = aws_instance.backend.id
  port             = 8081
}

resource "aws_cloudwatch_metric_alarm" "instance_status" {
  alarm_name          = "${local.name}-instance-status"
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "breaching"

  dimensions = {
    InstanceId = aws_instance.backend.id
  }
}
