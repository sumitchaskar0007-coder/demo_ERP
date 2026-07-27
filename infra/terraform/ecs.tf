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
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
        aws_secretsmanager_secret.application.arn,
        aws_secretsmanager_secret.redis.arn,
        aws_db_instance.postgres.master_user_secret[0].secret_arn
      ]
    }]
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
      }
    ]
  })
}

resource "aws_lb" "backend" {
  name                       = substr(local.name, 0, 32)
  internal                   = false
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = aws_subnet.public[*].id
  enable_deletion_protection = true
  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "backend" {
  name        = substr("${local.name}-api", 0, 32)
  port        = 8081
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

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
  common_environment = [
    { name = "SPRING_PROFILES_ACTIVE", value = "production" },
    { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:${aws_db_instance.postgres.port}/${var.db_name}?sslmode=verify-full" },
    { name = "REDIS_HOST", value = aws_elasticache_replication_group.redis.primary_endpoint_address },
    { name = "REDIS_PORT", value = tostring(aws_elasticache_replication_group.redis.port) },
    { name = "REDIS_SSL_ENABLED", value = "true" },
    { name = "AWS_REGION", value = var.aws_region },
    { name = "AWS_PRIVATE_UPLOAD_BUCKET", value = aws_s3_bucket.uploads.id },
    { name = "AWS_SECRETS_NAME", value = aws_secretsmanager_secret.application.name },
    { name = "FRONTEND_URL", value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name}" },
    { name = "CORS_ALLOWED_ORIGINS", value = var.temporary_domain ? "https://${aws_cloudfront_distribution.main.domain_name}" : "https://${var.domain_name}" },
    { name = "MAIL_ENABLED", value = tostring(var.mail_enabled) },
    { name = "DB_SSL_ROOT_CERT", value = "/etc/ssl/certs/rds-ca-bundle.pem" }
  ]

  base_runtime_secrets = [
    { name = "DB_USERNAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_USERNAME::" },
    { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_PASSWORD::" },
    { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.application.arn}:JWT_SECRET::" },
    { name = "RATE_LIMIT_KEY_SECRET", valueFrom = "${aws_secretsmanager_secret.application.arn}:RATE_LIMIT_KEY_SECRET::" },
    { name = "REDIS_PASSWORD", valueFrom = "${aws_secretsmanager_secret.redis.arn}:auth_token::" }
  ]

  mail_runtime_secrets = [
    { name = "MAIL_HOST", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_HOST::" },
    { name = "MAIL_USERNAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_USERNAME::" },
    { name = "MAIL_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_PASSWORD::" },
    { name = "MAIL_FROM_ADDRESS", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_FROM_ADDRESS::" }
  ]

  runtime_secrets = concat(local.base_runtime_secrets, var.mail_enabled ? local.mail_runtime_secrets : [])
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 1024
  memory                   = 2048
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name         = "backend"
    image        = var.backend_image
    essential    = true
    portMappings = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment = concat(local.common_environment, [
      { name = "FLYWAY_ENABLED", value = "false" },
      { name = "BOOTSTRAP_ENABLED", value = "false" }
    ])
    secrets                = local.runtime_secrets
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
    environment = concat(local.common_environment, [
      { name = "FLYWAY_ENABLED", value = "true" },
      { name = "BOOTSTRAP_ENABLED", value = "true" }
    ])
    secrets = concat([
      { name = "DB_USERNAME", valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:username::" },
      { name = "DB_PASSWORD", valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::" },
      { name = "SUPER_ADMIN_NAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_NAME::" },
      { name = "SUPER_ADMIN_EMAIL", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_EMAIL::" },
      { name = "SUPER_ADMIN_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:SUPER_ADMIN_PASSWORD::" }
    ], [for secret in local.runtime_secrets : secret if !contains(["DB_USERNAME", "DB_PASSWORD"], secret.name)])
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

resource "aws_ecs_task_definition" "database_role" {
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
      { name = "DB_HOST", value = aws_db_instance.postgres.address },
      { name = "DB_NAME", value = var.db_name },
      { name = "DB_MASTER_USERNAME", value = var.db_master_username }
    ]
    secrets = [
      { name = "PGPASSWORD", valueFrom = "${aws_db_instance.postgres.master_user_secret[0].secret_arn}:password::" },
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
    subnets          = aws_subnet.application[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8081
  }

  depends_on = [aws_lb_listener_rule.cloudfront_only]

  lifecycle {
    precondition {
      condition     = var.environment != "production" || var.desired_count >= 2
      error_message = "Production ECS service requires at least two tasks."
    }
  }
}
