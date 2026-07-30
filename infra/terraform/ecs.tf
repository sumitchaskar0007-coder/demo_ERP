resource "aws_ecr_repository" "backend" {
  name                 = "${local.name}-backend"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false
  image_scanning_configuration { scan_on_push = true }
  encryption_configuration { encryption_type = "AES256" }
}

resource "aws_ecs_cluster" "main" {
  name = local.name
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name}/backend"
  retention_in_days = 30
}

resource "aws_iam_role" "ecs_execution" {
  name = "${local.name}-ecs-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "read-task-secrets"
  role = aws_iam_role.ecs_execution.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [{
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = distinct([
          aws_secretsmanager_secret.application.arn,
          aws_secretsmanager_secret.mail.arn,
          aws_secretsmanager_secret.redis.arn,
          local.runtime_secret_arn,
          local.migration_secret_arn
        ])
      }],
      length(var.production_database_secret_kms_key_arns) == 0 ? [] : [{
        Effect   = "Allow"
        Action   = ["kms:Decrypt"]
        Resource = var.production_database_secret_kms_key_arns
      }]
    )
  })
}

resource "aws_iam_role" "ecs_task" {
  name = "${local.name}-ecs-task"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "ecs_uploads" {
  name = "private-upload-objects"
  role = aws_iam_role.ecs_task.id
  # Every object key is generated under a server-enforced tenant prefix. S3
  # object IAM resources cannot enumerate future tenant/UUID keys.
  #tfsec:ignore:aws-iam-no-policy-wildcards
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.uploads.arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:GetBucketLocation", "s3:ListBucket"]
        Resource = aws_s3_bucket.uploads.arn
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:DescribeKey",
          "kms:GenerateDataKey"
        ]
        Resource = aws_kms_key.uploads.arn
      }
    ]
  })
}

# CloudFront is the public edge. The ALB security group permits only the AWS
# CloudFront origin-facing prefix list, and the listener requires a secret
# origin-verification header before forwarding.
#tfsec:ignore:aws-elb-alb-not-public
resource "aws_lb" "backend" {
  name                       = substr(local.name, 0, 32)
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = local.public_subnet_ids
  enable_deletion_protection = true
  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "backend" {
  name        = substr("${local.name}-api", 0, 32)
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = local.vpc_id

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

resource "random_password" "origin_header" {
  length  = 48
  special = false
}

resource "aws_lb_listener" "http" {
  count             = var.temporary_domain ? 1 : 0
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Forbidden"
      status_code  = "403"
    }
  }
}

resource "aws_lb_listener" "https" {
  count             = var.temporary_domain ? 0 : 1
  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.regional[0].certificate_arn

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Forbidden"
      status_code  = "403"
    }
  }
}

resource "aws_lb_listener_rule" "cloudfront_only" {
  listener_arn = var.temporary_domain ? aws_lb_listener.http[0].arn : aws_lb_listener.https[0].arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [random_password.origin_header.result]
    }
  }
}

locals {
  fargate_memory_by_cpu = {
    "256"   = [512, 1024, 2048]
    "512"   = range(1024, 5120, 1024)
    "1024"  = range(2048, 9216, 1024)
    "2048"  = range(4096, 17408, 1024)
    "4096"  = range(8192, 31744, 1024)
    "8192"  = range(16384, 65536, 4096)
    "16384" = range(32768, 131072, 8192)
  }

  backend_effective_min_capacity = local.external_production && !var.production_database_access_ready ? 0 : var.backend_autoscaling_min_capacity
  async_worker_effective_count   = local.external_production && !var.production_database_access_ready ? 0 : var.async_worker_desired_count

  common_environment = [
    # Staging keeps its environment identity while inheriting every hardened
    # production setting and validator. Production activates only production.
    { name = "SPRING_PROFILES_ACTIVE", value = local.external_production ? "production" : "staging,production" },
    # Keep private uploads on S3 even if profile composition changes later.
    { name = "STORAGE_PROVIDER", value = "s3" },
    { name = "DB_URL", value = "jdbc:postgresql://${local.database_endpoint}:${local.database_port}/${local.database_name}?sslmode=verify-full" },
    { name = "REDIS_HOST", value = aws_elasticache_replication_group.redis.primary_endpoint_address },
    { name = "REDIS_PORT", value = tostring(aws_elasticache_replication_group.redis.port) },
    { name = "REDIS_SSL_ENABLED", value = "true" },
    { name = "RATE_LIMIT_REDIS_ENABLED", value = "true" },
    { name = "RATE_LIMIT_REQUIRED", value = "true" },
    { name = "AUTHORIZATION_CACHE_ENABLED", value = "true" },
    { name = "NOTICE_REDIS_ENABLED", value = "true" },
    { name = "CACHE_ENVIRONMENT", value = local.name },
    # The ALB security group accepts origin traffic only from CloudFront.
    { name = "CLOUDFRONT_VIEWER_ADDRESS_ENABLED", value = "true" },
    { name = "AWS_REGION", value = var.aws_region },
    { name = "AWS_PRIVATE_UPLOAD_BUCKET", value = aws_s3_bucket.uploads.id },
    { name = "AWS_SECRETS_NAME", value = aws_secretsmanager_secret.application.name },
    { name = "FRONTEND_URL", value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name}" },
    { name = "CORS_ALLOWED_ORIGINS", value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name},https://www.${var.domain_name}" },
    { name = "DB_SSL_ROOT_CERT", value = "/etc/ssl/certs/rds-ca-bundle.pem" }
  ]

  backend_pool_environment = [
    { name = "DB_POOL_MAX_SIZE", value = tostring(var.backend_db_pool_max_size) },
    { name = "DB_POOL_MIN_IDLE", value = tostring(var.backend_db_pool_min_idle) }
  ]

  migration_pool_environment = [
    { name = "DB_POOL_MAX_SIZE", value = "2" },
    { name = "DB_POOL_MIN_IDLE", value = "0" }
  ]

  async_worker_pool_environment = [
    { name = "DB_POOL_MAX_SIZE", value = tostring(var.async_worker_db_pool_max_size) },
    { name = "DB_POOL_MIN_IDLE", value = tostring(var.async_worker_db_pool_min_idle) }
  ]

  api_async_environment = [
    # API replicas persist and publish email IDs but never perform SMTP delivery.
    { name = "MAIL_ENABLED", value = tostring(var.async_queues_enabled ? false : var.mail_enabled) },
    { name = "MAIL_TRANSPORT", value = var.async_queues_enabled && var.mail_enabled ? "sqs" : "database" },
    { name = "EMAIL_SQS_PUBLISHER_ENABLED", value = tostring(var.async_queues_enabled && var.mail_enabled) },
    { name = "EMAIL_SQS_CONSUMER_ENABLED", value = "false" },
    { name = "EMAIL_SQS_RECOVERY_ENABLED", value = "false" },
    { name = "EMAIL_SQS_QUEUE_URL", value = var.async_queues_enabled && var.mail_enabled ? try(aws_sqs_queue.email[0].url, "") : "" },
    # API replicas create durable report jobs; only the worker consumes them.
    { name = "REPORT_EXPORTS_ENABLED", value = tostring(var.async_queues_enabled) },
    { name = "REPORT_DATABASE_POLL_ENABLED", value = "false" },
    { name = "REPORT_SQS_PRODUCER_ENABLED", value = tostring(var.async_queues_enabled) },
    { name = "REPORT_SQS_CONSUMER_ENABLED", value = "false" },
    { name = "REPORT_SQS_QUEUE_URL", value = var.async_queues_enabled ? try(aws_sqs_queue.report[0].url, "") : "" }
  ]

  migration_async_environment = [
    { name = "MAIL_ENABLED", value = "false" },
    { name = "MAIL_TRANSPORT", value = "database" },
    { name = "EMAIL_SQS_PUBLISHER_ENABLED", value = "false" },
    { name = "EMAIL_SQS_CONSUMER_ENABLED", value = "false" },
    { name = "EMAIL_SQS_RECOVERY_ENABLED", value = "false" },
    { name = "EMAIL_SQS_QUEUE_URL", value = "" },
    { name = "REPORT_EXPORTS_ENABLED", value = "false" },
    { name = "REPORT_DATABASE_POLL_ENABLED", value = "false" },
    { name = "REPORT_SQS_PRODUCER_ENABLED", value = "false" },
    { name = "REPORT_SQS_CONSUMER_ENABLED", value = "false" },
    { name = "REPORT_SQS_QUEUE_URL", value = "" }
  ]

  async_worker_environment = [
    { name = "MAIL_ENABLED", value = tostring(var.mail_enabled) },
    { name = "MAIL_TRANSPORT", value = var.mail_enabled ? "sqs" : "database" },
    # Exactly one worker performs durable email recovery until leader election exists.
    { name = "EMAIL_SQS_PUBLISHER_ENABLED", value = tostring(var.mail_enabled) },
    { name = "EMAIL_SQS_CONSUMER_ENABLED", value = tostring(var.mail_enabled) },
    { name = "EMAIL_SQS_RECOVERY_ENABLED", value = tostring(var.mail_enabled) },
    { name = "EMAIL_SQS_QUEUE_URL", value = var.mail_enabled ? try(aws_sqs_queue.email[0].url, "") : "" },
    { name = "REPORT_EXPORTS_ENABLED", value = "true" },
    { name = "REPORT_DATABASE_POLL_ENABLED", value = "true" },
    { name = "REPORT_SQS_PRODUCER_ENABLED", value = "false" },
    { name = "REPORT_SQS_CONSUMER_ENABLED", value = "true" },
    { name = "REPORT_SQS_QUEUE_URL", value = try(aws_sqs_queue.report[0].url, "") }
  ]

  database_runtime_secrets = local.manage_database ? [
    { name = "DB_USERNAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_USERNAME::" },
    { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_PASSWORD::" }
    ] : [
    { name = "DB_USERNAME", valueFrom = "${var.production_runtime_database_secret_arn}:username::" },
    { name = "DB_PASSWORD", valueFrom = "${var.production_runtime_database_secret_arn}:password::" }
  ]

  base_runtime_secrets = concat(local.database_runtime_secrets, [
    { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.application.arn}:JWT_SECRET::" },
    { name = "RATE_LIMIT_KEY_SECRET", valueFrom = "${aws_secretsmanager_secret.application.arn}:RATE_LIMIT_KEY_SECRET::" },
    { name = "REDIS_PASSWORD", valueFrom = "${aws_secretsmanager_secret.redis.arn}:auth_token::" }
  ])

  mail_runtime_secrets = [
    { name = "MAIL_HOST", valueFrom = "${aws_secretsmanager_secret.mail.arn}:MAIL_HOST::" },
    { name = "MAIL_USERNAME", valueFrom = "${aws_secretsmanager_secret.mail.arn}:MAIL_USERNAME::" },
    { name = "MAIL_PASSWORD", valueFrom = "${aws_secretsmanager_secret.mail.arn}:MAIL_PASSWORD::" },
    { name = "MAIL_FROM_ADDRESS", valueFrom = "${aws_secretsmanager_secret.mail.arn}:MAIL_FROM_ADDRESS::" }
  ]

  backend_runtime_secrets = concat(
    local.base_runtime_secrets,
    var.mail_enabled && !var.async_queues_enabled ? local.mail_runtime_secrets : []
  )

  async_worker_runtime_secrets = concat(
    local.base_runtime_secrets,
    var.mail_enabled ? local.mail_runtime_secrets : []
  )

  migration_bootstrap_secrets = local.manage_database ? [
    { name = "SUPER_ADMIN_NAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_NAME::" },
    { name = "SUPER_ADMIN_EMAIL", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_EMAIL::" },
    { name = "SUPER_ADMIN_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_PASSWORD::" }
  ] : []
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_task_cpu
  memory                   = var.backend_task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  lifecycle {
    precondition {
      condition = try(
        contains(local.fargate_memory_by_cpu[tostring(var.backend_task_cpu)], var.backend_task_memory),
        false
      )
      error_message = "backend_task_cpu and backend_task_memory must form a supported Fargate task size."
    }
  }

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name         = "backend"
    image        = var.backend_image
    essential    = true
    portMappings = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment = concat(
      local.common_environment,
      local.backend_pool_environment,
      local.api_async_environment,
      [
        { name = "FLYWAY_ENABLED", value = "false" },
        { name = "BOOTSTRAP_ENABLED", value = "false" }
      ]
    )
    secrets                = local.backend_runtime_secrets
    readonlyRootFilesystem = true
    linuxParameters = {
      initProcessEnabled = true
      tmpfs = [{
        containerPath = "/app/tmp"
        size          = 64
        mountOptions  = ["rw", "nosuid", "nodev", "noexec", "mode=1777"]
      }]
    }
    healthCheck = {
      command     = ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/liveness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "backend"
      }
    }
  }])
}

resource "aws_ecs_task_definition" "migration" {
  family                   = "${local.name}-migration"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = "migration"
    image     = var.backend_image
    essential = true
    command   = ["--app.migration-task=true", "--server.port=0"]
    environment = concat(
      local.common_environment,
      local.migration_pool_environment,
      local.migration_async_environment,
      [
        { name = "FLYWAY_ENABLED", value = "true" },
        { name = "FLYWAY_POSTGRESQL_TRANSACTIONAL_LOCK", value = "false" },
        { name = "BOOTSTRAP_ENABLED", value = tostring(local.manage_database) }
      ]
    )
    secrets = concat(
      [
        { name = "DB_USERNAME", valueFrom = "${local.migration_secret_arn}:username::" },
        { name = "DB_PASSWORD", valueFrom = "${local.migration_secret_arn}:password::" }
      ],
      local.migration_bootstrap_secrets,
      [for secret in local.base_runtime_secrets : secret if !contains(["DB_USERNAME", "DB_PASSWORD"], secret.name)]
    )
    readonlyRootFilesystem = true
    linuxParameters = {
      initProcessEnabled = true
      tmpfs = [{
        containerPath = "/app/tmp"
        size          = 64
        mountOptions  = ["rw", "nosuid", "nodev", "noexec", "mode=1777"]
      }]
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "migration"
      }
    }
  }])
}

resource "aws_ecs_task_definition" "async_worker" {
  count                    = var.async_queues_enabled ? 1 : 0
  family                   = "${local.name}-async-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.async_worker_task_cpu
  memory                   = var.async_worker_task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.async_worker[0].arn

  lifecycle {
    precondition {
      condition = try(
        contains(local.fargate_memory_by_cpu[tostring(var.async_worker_task_cpu)], var.async_worker_task_memory),
        false
      )
      error_message = "async_worker_task_cpu and async_worker_task_memory must form a supported Fargate task size."
    }
    precondition {
      condition     = var.async_worker_db_pool_min_idle <= var.async_worker_db_pool_max_size
      error_message = "async_worker_db_pool_min_idle cannot exceed async_worker_db_pool_max_size."
    }
    precondition {
      condition     = !var.mail_enabled || var.async_worker_desired_count == 1
      error_message = "Email recovery currently requires exactly one async worker task."
    }
  }

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name         = "worker"
    image        = var.backend_image
    essential    = true
    portMappings = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment = concat(
      local.common_environment,
      local.async_worker_pool_environment,
      local.async_worker_environment,
      [
        { name = "FLYWAY_ENABLED", value = "false" },
        { name = "BOOTSTRAP_ENABLED", value = "false" }
      ]
    )
    secrets                = local.async_worker_runtime_secrets
    readonlyRootFilesystem = true
    linuxParameters = {
      initProcessEnabled = true
      tmpfs = [{
        containerPath = "/app/tmp"
        size          = 128
        mountOptions  = ["rw", "nosuid", "nodev", "noexec", "mode=1777"]
      }]
    }
    healthCheck = {
      command     = ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/liveness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "worker"
      }
    }
  }])
}

resource "aws_ecs_task_definition" "database_role" {
  count                    = local.manage_database ? 1 : 0
  family                   = "${local.name}-database-role"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = "database-role"
    image     = "public.ecr.aws/docker/library/postgres:17-alpine"
    essential = true
    command = [
      "sh",
      "-ec",
      <<-EOT
        psql --set=ON_ERROR_STOP=1 \
          --host="$DB_HOST" \
          --username="$DB_MASTER_USERNAME" \
          --dbname="$DB_NAME" \
          --set=app_password="$DB_APP_PASSWORD" <<'SQL'
        SELECT format('CREATE ROLE erp_app LOGIN PASSWORD %L', :'app_password')
        WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'erp_app') \gexec
        SELECT format('ALTER ROLE erp_app PASSWORD %L', :'app_password') \gexec
        GRANT CONNECT ON DATABASE college_erp TO erp_app;
        GRANT USAGE ON SCHEMA public TO erp_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO erp_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO erp_app;
        ALTER DEFAULT PRIVILEGES FOR ROLE ${var.db_master_username} IN SCHEMA public
          GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO erp_app;
        ALTER DEFAULT PRIVILEGES FOR ROLE ${var.db_master_username} IN SCHEMA public
          GRANT USAGE, SELECT ON SEQUENCES TO erp_app;
        SQL
      EOT
    ]
    environment = [
      { name = "DB_HOST", value = local.database_endpoint },
      { name = "DB_NAME", value = local.database_name },
      { name = "DB_MASTER_USERNAME", value = var.db_master_username }
    ]
    secrets = [
      { name = "PGPASSWORD", valueFrom = "${aws_db_instance.postgres[0].master_user_secret[0].secret_arn}:password::" },
      { name = "DB_APP_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_PASSWORD::" }
    ]
    readonlyRootFilesystem = true
    linuxParameters = {
      initProcessEnabled = true
      tmpfs = [{
        containerPath = "/tmp"
        size          = 32
        mountOptions  = ["rw", "nosuid", "nodev", "noexec"]
      }]
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "database-role"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = 90
  enable_execute_command             = false

  network_configuration {
    subnets          = local.backend_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8081
  }

  depends_on = [
    aws_lb_listener_rule.cloudfront_only,
    aws_iam_role_policy.ecs_api_async_queues,
    terraform_data.production_contract
  ]

  lifecycle {
    # Application Auto Scaling owns desired_count after service creation. The
    # production activation gate is enforced by the scalable target minimum.
    ignore_changes = [desired_count]

    precondition {
      condition = !local.external_production || (
        var.production_database_access_ready ? var.desired_count >= 2 : var.desired_count == 0
      )
      error_message = "Production requires desired_count=0 until production_database_access_ready=true; after approval it requires at least two tasks."
    }
    precondition {
      condition     = var.desired_count <= var.backend_autoscaling_max_capacity
      error_message = "desired_count cannot exceed backend_autoscaling_max_capacity."
    }
    precondition {
      condition     = var.backend_db_pool_min_idle <= var.backend_db_pool_max_size
      error_message = "backend_db_pool_min_idle cannot exceed backend_db_pool_max_size."
    }
    precondition {
      condition = (
        var.backend_autoscaling_max_capacity * var.backend_db_pool_max_size +
        (var.async_queues_enabled ? var.async_worker_desired_count * var.async_worker_db_pool_max_size : 0)
      ) <= var.database_connection_budget
      error_message = "Maximum API and worker Hikari connections exceed database_connection_budget."
    }
    precondition {
      condition = (
        var.backend_peak_capacity >= var.backend_autoscaling_min_capacity &&
        var.backend_peak_capacity <= var.backend_autoscaling_max_capacity
      )
      error_message = "backend_peak_capacity must be between the autoscaling minimum and maximum capacities."
    }
    precondition {
      condition     = !local.external_production || var.production_database_access_ready || !var.backend_peak_schedule_enabled
      error_message = "Production scheduled scaling must remain disabled until production_database_access_ready=true."
    }
    precondition {
      condition     = !local.external_production || !var.mail_enabled || var.async_queues_enabled
      error_message = "Production email requires async_queues_enabled=true so API replicas never perform SMTP delivery."
    }
  }
}

resource "aws_ecs_service" "async_worker" {
  count           = var.async_queues_enabled ? 1 : 0
  name            = "${local.name}-async-worker"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.async_worker[0].arn
  desired_count   = local.async_worker_effective_count
  launch_type     = "FARGATE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  enable_execute_command             = false

  network_configuration {
    subnets          = local.backend_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  depends_on = [
    aws_iam_role_policy.async_worker_queues,
    aws_iam_role_policy.async_worker_storage,
    terraform_data.production_contract
  ]

  lifecycle {
    precondition {
      condition = !local.external_production || (
        var.production_database_access_ready ? var.async_worker_desired_count >= 1 : local.async_worker_effective_count == 0
      )
      error_message = "Production async workers remain at zero until production_database_access_ready=true."
    }
  }
}

resource "aws_appautoscaling_target" "backend" {
  max_capacity       = var.backend_autoscaling_max_capacity
  min_capacity       = local.backend_effective_min_capacity
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "backend_cpu" {
  name               = "${local.name}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.backend.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = var.backend_autoscaling_cpu_target
    scale_out_cooldown = 60
    scale_in_cooldown  = 300

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

resource "aws_appautoscaling_policy" "backend_memory" {
  name               = "${local.name}-backend-memory"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.backend.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = var.backend_autoscaling_memory_target
    scale_out_cooldown = 60
    scale_in_cooldown  = 300

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
  }
}

resource "aws_appautoscaling_policy" "backend_requests" {
  name               = "${local.name}-backend-requests"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.backend.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = var.backend_requests_per_target
    scale_out_cooldown = 60
    scale_in_cooldown  = 300

    predefined_metric_specification {
      predefined_metric_type = "ALBRequestCountPerTarget"
      resource_label         = "${aws_lb.backend.arn_suffix}/${aws_lb_target_group.backend.arn_suffix}"
    }
  }
}

resource "aws_appautoscaling_scheduled_action" "backend_peak_start" {
  count              = var.backend_peak_schedule_enabled ? 1 : 0
  name               = "${local.name}-backend-peak-start"
  service_namespace  = aws_appautoscaling_target.backend.service_namespace
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  schedule           = var.backend_peak_scale_out_schedule
  timezone           = "Asia/Kolkata"

  scalable_target_action {
    min_capacity = var.backend_peak_capacity
    max_capacity = var.backend_autoscaling_max_capacity
  }
}

resource "aws_appautoscaling_scheduled_action" "backend_peak_end" {
  count              = var.backend_peak_schedule_enabled ? 1 : 0
  name               = "${local.name}-backend-peak-end"
  service_namespace  = aws_appautoscaling_target.backend.service_namespace
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension
  schedule           = var.backend_peak_scale_in_schedule
  timezone           = "Asia/Kolkata"

  scalable_target_action {
    min_capacity = local.backend_effective_min_capacity
    max_capacity = var.backend_autoscaling_max_capacity
  }
}

moved {
  from = aws_ecs_task_definition.database_role
  to   = aws_ecs_task_definition.database_role[0]
}
