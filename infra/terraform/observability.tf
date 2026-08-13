# The AWS-managed SNS key avoids a paid per-environment CMK for non-sensitive
# operational alarm metadata. Topic access remains IAM-restricted.
#tfsec:ignore:aws-sns-topic-encryption-use-cmk
resource "aws_sns_topic" "alerts" {
  name              = "${local.name}-alerts"
  kms_master_key_id = "alias/aws/sns"
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email_subscription_enabled && var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_metric_alarm" "unhealthy_targets" {
  alarm_name          = "${local.name}-unhealthy-targets"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "UnHealthyHostCount"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
    TargetGroup  = aws_lb_target_group.backend.arn_suffix
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
  ok_actions    = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  alarm_name          = "${local.name}-alb-5xx"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_ELB_5XX_Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { LoadBalancer = aws_lb.backend.arn_suffix }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "alb_target_5xx" {
  alarm_name          = "${local.name}-alb-target-5xx"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
    TargetGroup  = aws_lb_target_group.backend.arn_suffix
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

# ALB TargetResponseTime is a native metric and supports percentile extended
# statistics. These alarms intentionally exclude externally owned RDS metrics.
resource "aws_cloudwatch_metric_alarm" "alb_target_latency" {
  for_each = {
    p95 = {
      extended_statistic = "p95"
      threshold          = var.alb_latency_p95_alarm_seconds
    }
    p99 = {
      extended_statistic = "p99"
      threshold          = var.alb_latency_p99_alarm_seconds
    }
  }

  alarm_name          = "${local.name}-alb-target-latency-${each.key}"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "TargetResponseTime"
  extended_statistic  = each.value.extended_statistic
  period              = 60
  evaluation_periods  = 5
  threshold           = each.value.threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
    TargetGroup  = aws_lb_target_group.backend.arn_suffix
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

# RequestLoggingFilter emits the exact phrase matched here for every response,
# so this metric is backed by a real application log source.
resource "aws_cloudwatch_log_metric_filter" "application_429" {
  name           = "${local.name}-application-429"
  log_group_name = aws_cloudwatch_log_group.backend.name
  pattern        = "\"completed with status 429\""

  metric_transformation {
    name      = "Http429Count"
    namespace = "JadhavrERP/${var.environment}"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "application_429" {
  alarm_name          = "${local.name}-application-429"
  namespace           = "JadhavrERP/${var.environment}"
  metric_name         = "Http429Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 25
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

# HikariCP emits this stable timeout phrase in its SQLException, and the global
# exception handler writes unexpected exception stacks to the backend log.
resource "aws_cloudwatch_log_metric_filter" "hikari_timeouts" {
  name           = "${local.name}-hikari-timeouts"
  log_group_name = aws_cloudwatch_log_group.backend.name
  pattern        = "\"Connection is not available, request timed out after\""

  metric_transformation {
    name      = "HikariTimeoutCount"
    namespace = "JadhavrERP/${var.environment}"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "hikari_timeouts" {
  alarm_name          = "${local.name}-hikari-timeouts"
  namespace           = "JadhavrERP/${var.environment}"
  metric_name         = "HikariTimeoutCount"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  count               = local.manage_database ? 1 : 0
  alarm_name          = "${local.name}-rds-cpu"
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.postgres[0].identifier }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_storage" {
  count               = local.manage_database ? 1 : 0
  alarm_name          = "${local.name}-rds-low-storage"
  namespace           = "AWS/RDS"
  metric_name         = "FreeStorageSpace"
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 2
  threshold           = 10737418240
  comparison_operator = "LessThanThreshold"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.postgres[0].identifier }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "rds_connections" {
  count               = local.manage_database ? 1 : 0
  alarm_name          = "${local.name}-rds-connections"
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 2
  threshold           = 150
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.postgres[0].identifier }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  count               = var.cache_cluster_count
  alarm_name          = "${local.name}-redis-${count.index + 1}-cpu"
  namespace           = "AWS/ElastiCache"
  metric_name         = "EngineCPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 75
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    CacheClusterId = tolist(aws_elasticache_replication_group.redis.member_clusters)[count.index]
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  count               = var.cache_cluster_count
  alarm_name          = "${local.name}-redis-${count.index + 1}-memory"
  namespace           = "AWS/ElastiCache"
  metric_name         = "DatabaseMemoryUsagePercentage"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 75
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    CacheClusterId = tolist(aws_elasticache_replication_group.redis.member_clusters)[count.index]
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  count               = var.cache_cluster_count
  alarm_name          = "${local.name}-redis-${count.index + 1}-evictions"
  namespace           = "AWS/ElastiCache"
  metric_name         = "Evictions"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    CacheClusterId = tolist(aws_elasticache_replication_group.redis.member_clusters)[count.index]
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "ecs_utilization" {
  for_each = {
    cpu = {
      metric_name = "CPUUtilization"
      threshold   = 70
    }
    memory = {
      metric_name = "MemoryUtilization"
      threshold   = 75
    }
  }

  alarm_name          = "${local.name}-ecs-${each.key}"
  namespace           = "AWS/ECS"
  metric_name         = each.value.metric_name
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = each.value.threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.backend.name
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "ecs_running_tasks" {
  alarm_name          = "${local.name}-ecs-running-tasks"
  namespace           = "ECS/ContainerInsights"
  metric_name         = "RunningTaskCount"
  statistic           = "Minimum"
  period              = 60
  evaluation_periods  = 2
  threshold           = local.backend_effective_min_capacity
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = local.backend_effective_min_capacity == 0 ? "notBreaching" : "breaching"
  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.backend.name
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "async_worker_utilization" {
  for_each = var.async_queues_enabled ? {
    cpu = {
      metric_name = "CPUUtilization"
      threshold   = 70
    }
    memory = {
      metric_name = "MemoryUtilization"
      threshold   = 75
    }
  } : {}

  alarm_name          = "${local.name}-async-worker-${each.key}"
  namespace           = "AWS/ECS"
  metric_name         = each.value.metric_name
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = each.value.threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.async_worker[0].name
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "async_worker_running_tasks" {
  count               = var.async_queues_enabled ? 1 : 0
  alarm_name          = "${local.name}-async-worker-running-tasks"
  namespace           = "ECS/ContainerInsights"
  metric_name         = "RunningTaskCount"
  statistic           = "Minimum"
  period              = 60
  evaluation_periods  = 2
  threshold           = local.async_worker_effective_count
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = local.async_worker_effective_count == 0 ? "notBreaching" : "breaching"
  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.async_worker[0].name
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_dashboard" "capacity" {
  dashboard_name = "${local.name}-capacity"
  dashboard_body = jsonencode({
    start          = "-PT6H"
    periodOverride = "inherit"
    widgets = [
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "ECS capacity"
          region = var.aws_region
          period = 60
          metrics = concat(
            [
              ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.backend.name],
              [".", "MemoryUtilization", ".", ".", ".", "."],
              ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.backend.name]
            ],
            var.async_queues_enabled ? [
              ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.async_worker[0].name, { label = "Worker CPU" }],
              [".", "MemoryUtilization", ".", ".", ".", ".", { label = "Worker memory" }],
              ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.async_worker[0].name, { label = "Worker tasks" }]
            ] : []
          )
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "ALB latency and target errors"
          region = var.aws_region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.backend.arn_suffix, "TargetGroup", aws_lb_target_group.backend.arn_suffix, { stat = "p95", label = "p95 latency" }],
            [".", ".", ".", ".", ".", ".", { stat = "p99", label = "p99 latency" }],
            [".", "HTTPCode_Target_5XX_Count", ".", ".", ".", ".", { stat = "Sum", label = "Target 5xx" }]
          ]
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "Redis capacity"
          region = var.aws_region
          period = 60
          metrics = concat(
            [for cluster in tolist(aws_elasticache_replication_group.redis.member_clusters) :
            ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "CacheClusterId", cluster]],
            [for cluster in tolist(aws_elasticache_replication_group.redis.member_clusters) :
            ["AWS/ElastiCache", "Evictions", "CacheClusterId", cluster, { stat = "Sum" }]]
          )
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "Application pressure signals"
          region = var.aws_region
          period = 60
          metrics = [
            ["JadhavrERP/${var.environment}", "Http429Count", { stat = "Sum" }],
            [".", "HikariTimeoutCount", { stat = "Sum" }]
          ]
        }
      },
      {
        type   = "text"
        width  = 24
        height = 2
        properties = {
          markdown = "Production RDS infrastructure and monitoring are externally owned. Correlate this dashboard with the RDS owner's approved dashboard during staging load tests."
        }
      }
    ]
  })
}

moved {
  from = aws_cloudwatch_metric_alarm.rds_cpu
  to   = aws_cloudwatch_metric_alarm.rds_cpu[0]
}

moved {
  from = aws_cloudwatch_metric_alarm.rds_storage
  to   = aws_cloudwatch_metric_alarm.rds_storage[0]
}

moved {
  from = aws_cloudwatch_metric_alarm.rds_connections
  to   = aws_cloudwatch_metric_alarm.rds_connections[0]
}

resource "aws_iam_policy" "deployment" {
  name        = "${local.name}-deployment"
  description = "Scoped permissions for immutable frontend/backend release deployment"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat([
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability", "ecr:CompleteLayerUpload",
          "ecr:DescribeImages",
          "ecr:InitiateLayerUpload",
          "ecr:PutImage", "ecr:UploadLayerPart"
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
        Action   = ["s3:DeleteObject", "s3:GetBucketLocation", "s3:GetObject", "s3:ListBucket", "s3:PutObject"]
        Resource = [aws_s3_bucket.frontend.arn, "${aws_s3_bucket.frontend.arn}/*"]
      },
      {
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:DescribeTasks",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition"
        ]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        Resource = "${aws_ecs_task_definition.migration.arn_without_revision}:*"
      }
      ], [
      {
        Effect = "Allow"
        Action = ["ecs:UpdateService"]
        Resource = concat(
          [aws_ecs_service.backend.id],
          var.async_queues_enabled ? [aws_ecs_service.async_worker[0].id] : []
        )
      },
      {
        Effect = "Allow"
        Action = ["iam:PassRole"]
        Resource = concat(
          [aws_iam_role.ecs_execution.arn, aws_iam_role.ecs_task.arn],
          var.async_queues_enabled ? [aws_iam_role.async_worker[0].arn] : []
        )
        Condition = { StringEquals = { "iam:PassedToService" = "ecs-tasks.amazonaws.com" } }
      },
      {
        Effect   = "Allow"
        Action   = ["cloudfront:CreateInvalidation", "cloudfront:GetInvalidation"]
        Resource = aws_cloudfront_distribution.main.arn
      }
    ])
  })
}
