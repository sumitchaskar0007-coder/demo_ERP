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

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.regional.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
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
    { name = "FRONTEND_URL", value = "https://${var.domain_name}" },
    { name = "CORS_ALLOWED_ORIGINS", value = "https://${var.domain_name}" },
    { name = "DB_SSL_ROOT_CERT", value = "/etc/ssl/certs/rds-ca-bundle.pem" },
    { name = "FLYWAY_ENABLED", value = "false" },
    { name = "BOOTSTRAP_ENABLED", value = "false" }
  ]

  runtime_secrets = [
    { name = "DB_USERNAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_USERNAME::" },
    { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:DB_APP_PASSWORD::" },
    { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.application.arn}:JWT_SECRET::" },
    { name = "MAIL_HOST", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_HOST::" },
    { name = "MAIL_USERNAME", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_USERNAME::" },
    { name = "MAIL_PASSWORD", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_PASSWORD::" },
    { name = "MAIL_FROM_ADDRESS", valueFrom = "${aws_secretsmanager_secret.application.arn}:MAIL_FROM_ADDRESS::" },
    { name = "REDIS_PASSWORD", valueFrom = "${aws_secretsmanager_secret.redis.arn}:auth_token::" }
  ]
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
    name                   = "backend"
    image                  = var.backend_image
    essential              = true
    portMappings           = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment            = local.common_environment
    secrets                = local.runtime_secrets
    readonlyRootFilesystem = true
    linuxParameters        = { initProcessEnabled = true }
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
    command   = ["--spring.main.web-application-type=none"]
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

  depends_on = [aws_lb_listener.https]
}
