# Green is an additive deployment slot. Creating it does not change the Blue
# service, listener rule, database, uploads bucket, DNS, or normal user traffic.
# A high-entropy query value is required to reach Green through the existing
# CloudFront -> ALB path during validation.

resource "terraform_data" "green_contract" {
  count = var.green_enabled ? 1 : 0

  input = {
    enabled        = var.green_enabled
    desired_count  = var.green_desired_count
    task_cpu       = var.green_backend_task_cpu
    task_memory    = var.green_backend_task_memory
    worker_enabled = var.green_worker_enabled
    worker_active  = var.green_worker_active
  }

  lifecycle {
    precondition {
      condition     = !var.green_enabled || var.environment == "production"
      error_message = "The isolated Green slot is only supported by the production stack."
    }
    precondition {
      condition     = !var.green_enabled || var.green_backend_image != ""
      error_message = "green_backend_image must be an immutable ARM64 ECR digest before Green is enabled."
    }
    precondition {
      condition     = !var.green_enabled || var.production_database_access_ready
      error_message = "Green cannot start until production database access is approved."
    }
    precondition {
      condition     = !var.green_enabled || var.green_desired_count >= 2
      error_message = "An enabled Green slot requires at least two API tasks across the two public subnets."
    }
    precondition {
      condition     = !var.green_cutover_enabled || var.green_enabled
      error_message = "green_cutover_enabled requires the Green slot to remain enabled."
    }
    precondition {
      condition     = !var.green_worker_enabled || var.async_queues_enabled
      error_message = "green_worker_enabled requires the production async queues."
    }
    precondition {
      condition     = !var.green_worker_active || var.green_worker_enabled
      error_message = "green_worker_active requires a validated Green worker."
    }
    precondition {
      condition     = !var.green_worker_maintenance_enabled || var.green_worker_active
      error_message = "Green worker recovery/polling requires active queue consumption."
    }
    precondition {
      condition = !var.green_worker_enabled || try(
        contains(local.fargate_memory_by_cpu[tostring(var.green_worker_task_cpu)], var.green_worker_task_memory),
        false
      )
      error_message = "green_worker_task_cpu and green_worker_task_memory must form a supported Fargate task size."
    }
    precondition {
      condition = !var.green_enabled || try(
        contains(local.fargate_memory_by_cpu[tostring(var.green_backend_task_cpu)], var.green_backend_task_memory),
        false
      )
      error_message = "green_backend_task_cpu and green_backend_task_memory must form a supported Fargate task size."
    }
  }
}

resource "random_password" "green_probe" {
  count   = var.green_enabled ? 1 : 0
  length  = 48
  special = false
}

resource "random_password" "green_worker_probe" {
  count   = var.green_enabled && var.green_worker_enabled ? 1 : 0
  length  = 48
  special = false
}

resource "random_password" "green_redis" {
  count   = var.green_enabled ? 1 : 0
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "green_redis" {
  count                   = var.green_enabled ? 1 : 0
  name                    = "${local.name}/green/redis"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret_version" "green_redis" {
  count     = var.green_enabled ? 1 : 0
  secret_id = aws_secretsmanager_secret.green_redis[0].id
  secret_string = jsonencode({
    username = "default"
    password = random_password.green_redis[0].result
  })
}

resource "aws_security_group" "green_ecs" {
  count       = var.green_enabled ? 1 : 0
  name        = "${local.name}-green-ecs"
  description = "Green API tasks accept traffic only from the production ALB"
  vpc_id      = local.vpc_id

  ingress {
    description     = "Green API from production ALB"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Green tasks receive public IPv4 addresses only for outbound AWS API and
  # repository access. This security group permits no public inbound traffic.
  #tfsec:ignore:aws-ec2-no-public-egress-sgr
  egress {
    description = "TLS to AWS APIs and approved internet dependencies"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "egress" {
    for_each = var.mail_enabled && (
      !var.async_queues_enabled ||
      (var.green_worker_enabled && var.green_worker_active)
    ) ? [1] : []
    content {
      description = "TLS SMTP submission for the active mail delivery role"
      from_port   = 587
      to_port     = 587
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  egress {
    description = "PostgreSQL inside the production VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [local.vpc_cidr]
  }

  egress {
    description = "Valkey TLS inside the production VPC"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [local.vpc_cidr]
  }

  egress {
    description = "VPC DNS over UDP"
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = [local.vpc_cidr]
  }

  egress {
    description = "VPC DNS over TCP"
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = [local.vpc_cidr]
  }
}

resource "aws_security_group" "green_database" {
  count       = var.green_enabled && local.manage_database ? 1 : 0
  name        = "${local.name}-green-database"
  description = "PostgreSQL access only from Green API tasks"
  vpc_id      = local.vpc_id

  ingress {
    description     = "PostgreSQL from Green API tasks"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.green_ecs[0].id]
  }
}

resource "aws_security_group" "green_redis" {
  count       = var.green_enabled ? 1 : 0
  name        = "${local.name}-green-redis"
  description = "Green serverless Valkey accepts TLS only from Green API tasks"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.green_ecs[0].id]
  }
}

resource "aws_elasticache_user" "green" {
  count         = var.green_enabled ? 1 : 0
  user_id       = "${local.name}-green-default"
  user_name     = "default"
  access_string = "on ~* +@all"
  engine        = "valkey"

  authentication_mode {
    type      = "password"
    passwords = [random_password.green_redis[0].result]
  }
}

resource "aws_elasticache_user_group" "green" {
  count         = var.green_enabled ? 1 : 0
  engine        = "valkey"
  user_group_id = "${local.name}-green"
  user_ids      = [aws_elasticache_user.green[0].user_id]
}

resource "aws_elasticache_serverless_cache" "green" {
  count                    = var.green_enabled ? 1 : 0
  name                     = "${local.name}-green"
  description              = "Isolated Green Valkey for sessions, rate limits, and distributed locks"
  engine                   = "valkey"
  major_engine_version     = "8"
  network_type             = "ipv4"
  subnet_ids               = local.cache_subnet_ids
  security_group_ids       = [aws_security_group.green_redis[0].id]
  user_group_id            = aws_elasticache_user_group.green[0].user_group_id
  snapshot_retention_limit = 1

  cache_usage_limits {
    data_storage {
      maximum = var.green_cache_max_storage_gib
      unit    = "GB"
    }
    ecpu_per_second {
      maximum = var.green_cache_max_ecpu_per_second
    }
  }
}

resource "aws_cloudwatch_log_group" "green_backend" {
  count             = var.green_enabled ? 1 : 0
  name              = "/ecs/${local.name}/green-backend"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "green_worker" {
  count             = var.green_enabled && var.green_worker_enabled ? 1 : 0
  name              = "/ecs/${local.name}/green-worker"
  retention_in_days = 14
}

resource "aws_iam_role" "green_ecs_execution" {
  count = var.green_enabled ? 1 : 0
  name  = "${local.name}-green-ecs-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "green_ecs_execution" {
  count      = var.green_enabled ? 1 : 0
  role       = aws_iam_role.green_ecs_execution[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "green_ecs_execution_secrets" {
  count = var.green_enabled ? 1 : 0
  name  = "read-green-task-secrets"
  role  = aws_iam_role.green_ecs_execution[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [{
        Effect = "Allow"
        Action = ["secretsmanager:GetSecretValue"]
        Resource = distinct([
          aws_secretsmanager_secret.application.arn,
          aws_secretsmanager_secret.mail.arn,
          aws_secretsmanager_secret.green_redis[0].arn,
          local.runtime_secret_arn
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

locals {
  green_common_environment = concat(
    [for item in local.common_environment : item if !contains(["REDIS_HOST", "REDIS_PORT", "CACHE_ENVIRONMENT"], item.name)],
    var.green_enabled ? [
      { name = "REDIS_HOST", value = try(aws_elasticache_serverless_cache.green[0].endpoint[0].address, "") },
      { name = "REDIS_PORT", value = tostring(try(aws_elasticache_serverless_cache.green[0].endpoint[0].port, 6379)) },
      { name = "CACHE_ENVIRONMENT", value = "production-green" }
    ] : []
  )

  green_backend_runtime_secrets = concat(
    [for secret in local.backend_runtime_secrets : secret if secret.name != "REDIS_PASSWORD"],
    var.green_enabled ? [{
      name      = "REDIS_PASSWORD"
      valueFrom = "${aws_secretsmanager_secret.green_redis[0].arn}:password::"
    }] : []
  )

  green_worker_runtime_secrets = concat(
    [for secret in local.async_worker_runtime_secrets : secret if secret.name != "REDIS_PASSWORD"],
    var.green_enabled && var.green_worker_enabled ? [{
      name      = "REDIS_PASSWORD"
      valueFrom = "${aws_secretsmanager_secret.green_redis[0].arn}:password::"
    }] : []
  )

  green_worker_async_environment = [
    { name = "MAIL_ENABLED", value = tostring(var.green_worker_active && var.mail_enabled) },
    { name = "MAIL_TRANSPORT", value = var.green_worker_active && var.mail_enabled ? "sqs" : "database" },
    { name = "MAIL_REPLY_TO", value = var.mail_reply_to != "" ? var.mail_reply_to : local.mail_from_address },
    { name = "EMAIL_SQS_PUBLISHER_ENABLED", value = tostring(var.green_worker_active && var.mail_enabled) },
    { name = "EMAIL_SQS_CONSUMER_ENABLED", value = tostring(var.green_worker_active && var.mail_enabled) },
    { name = "EMAIL_SQS_RECOVERY_ENABLED", value = tostring(var.green_worker_maintenance_enabled && var.mail_enabled) },
    { name = "EMAIL_SQS_QUEUE_URL", value = var.mail_enabled ? try(aws_sqs_queue.email[0].url, "") : "" },
    { name = "REPORT_EXPORTS_ENABLED", value = tostring(var.green_worker_maintenance_enabled) },
    { name = "REPORT_DATABASE_POLL_ENABLED", value = tostring(var.green_worker_maintenance_enabled) },
    { name = "REPORT_SQS_PRODUCER_ENABLED", value = "false" },
    { name = "REPORT_SQS_CONSUMER_ENABLED", value = tostring(var.green_worker_maintenance_enabled) },
    { name = "REPORT_SQS_QUEUE_URL", value = try(aws_sqs_queue.report[0].url, "") }
  ]
}

resource "aws_ecs_task_definition" "green_backend" {
  count                    = var.green_enabled ? 1 : 0
  family                   = "${local.name}-green-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.green_backend_task_cpu
  memory                   = var.green_backend_task_memory
  execution_role_arn       = aws_iam_role.green_ecs_execution[0].arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }

  container_definitions = jsonencode([{
    name         = "backend"
    image        = var.green_backend_image
    essential    = true
    portMappings = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment = concat(
      local.green_common_environment,
      local.backend_pool_environment,
      local.api_async_environment,
      [
        { name = "FLYWAY_ENABLED", value = "false" },
        { name = "BOOTSTRAP_ENABLED", value = "false" }
      ]
    )
    secrets                = local.green_backend_runtime_secrets
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
      startPeriod = 90
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.green_backend[0].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "backend"
      }
    }
  }])

  depends_on = [terraform_data.green_contract]
}

resource "aws_ecs_task_definition" "green_worker" {
  count                    = var.green_enabled && var.green_worker_enabled ? 1 : 0
  family                   = "${local.name}-green-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.green_worker_task_cpu
  memory                   = var.green_worker_task_memory
  execution_role_arn       = aws_iam_role.green_ecs_execution[0].arn
  task_role_arn            = aws_iam_role.async_worker[0].arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64"
  }

  container_definitions = jsonencode([{
    name         = "worker"
    image        = var.green_backend_image
    essential    = true
    portMappings = [{ containerPort = 8081, hostPort = 8081, protocol = "tcp" }]
    environment = concat(
      local.green_common_environment,
      local.async_worker_pool_environment,
      local.green_worker_async_environment,
      [
        { name = "FLYWAY_ENABLED", value = "false" },
        { name = "BOOTSTRAP_ENABLED", value = "false" }
      ]
    )
    secrets                = local.green_worker_runtime_secrets
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
      startPeriod = 90
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.green_worker[0].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "worker"
      }
    }
  }])

  depends_on = [terraform_data.green_contract]

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_target_group" "green_worker" {
  count                = var.green_enabled && var.green_worker_enabled ? 1 : 0
  name                 = substr("${local.name}-green-worker", 0, 32)
  port                 = 8081
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = local.vpc_id
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

resource "aws_lb_listener_rule" "green_worker_probe" {
  count        = var.green_enabled && var.green_worker_enabled ? 1 : 0
  listener_arn = var.temporary_domain ? aws_lb_listener.http[0].arn : aws_lb_listener.https[0].arn
  priority     = 4

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.green_worker[0].arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [random_password.origin_header.result]
    }
  }

  condition {
    query_string {
      key   = "deployment-worker-slot"
      value = random_password.green_worker_probe[0].result
    }
  }
}

resource "aws_lb_target_group" "green_backend" {
  count                = var.green_enabled ? 1 : 0
  name                 = substr("${local.name}-green-api", 0, 32)
  port                 = 8081
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = local.vpc_id
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

resource "aws_lb_listener_rule" "green_probe" {
  count        = var.green_enabled ? 1 : 0
  listener_arn = var.temporary_domain ? aws_lb_listener.http[0].arn : aws_lb_listener.https[0].arn
  priority     = 5

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.green_backend[0].arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [random_password.origin_header.result]
    }
  }

  condition {
    query_string {
      key   = "deployment-slot"
      value = random_password.green_probe[0].result
    }
  }
}

resource "aws_lb_listener_rule" "green_primary" {
  count        = var.green_enabled && var.green_cutover_enabled ? 1 : 0
  listener_arn = var.temporary_domain ? aws_lb_listener.http[0].arn : aws_lb_listener.https[0].arn
  priority     = 7

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.green_backend[0].arn
  }

  condition {
    http_header {
      http_header_name = "X-Origin-Verify"
      values           = [random_password.origin_header.result]
    }
  }
}

resource "aws_ecs_service" "green_worker" {
  count           = var.green_enabled && var.green_worker_enabled ? 1 : 0
  name            = "${local.name}-green-worker"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.green_worker[0].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = 240
  enable_execute_command             = false

  network_configuration {
    subnets          = local.public_subnet_ids
    security_groups  = [aws_security_group.green_ecs[0].id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.green_worker[0].arn
    container_name   = "worker"
    container_port   = 8081
  }

  depends_on = [
    aws_lb_listener_rule.green_worker_probe,
    aws_iam_role_policy.green_ecs_execution_secrets,
    aws_iam_role_policy.async_worker_queues,
    aws_iam_role_policy.async_worker_storage,
    terraform_data.green_contract
  ]
}

resource "aws_ecs_service" "green_backend" {
  count           = var.green_enabled ? 1 : 0
  name            = "${local.name}-green-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.green_backend[0].arn
  desired_count   = var.green_desired_count
  launch_type     = "FARGATE"

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  health_check_grace_period_seconds  = 240
  enable_execute_command             = false

  network_configuration {
    subnets          = local.public_subnet_ids
    security_groups  = [aws_security_group.green_ecs[0].id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.green_backend[0].arn
    container_name   = "backend"
    container_port   = 8081
  }

  depends_on = [
    aws_lb_listener_rule.green_probe,
    aws_lb_listener_rule.green_primary,
    aws_iam_role_policy.green_ecs_execution_secrets,
    terraform_data.green_contract
  ]

  lifecycle {
    ignore_changes = [task_definition]
  }
}
