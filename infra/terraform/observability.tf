resource "aws_sns_topic" "alerts" {
  name = "${local.name}-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email == "" ? 0 : 1
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

resource "aws_cloudwatch_metric_alarm" "alb_target_latency_p95" {
  alarm_name          = "${local.name}-alb-target-latency-p95"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "TargetResponseTime"
  extended_statistic  = "p95"
  period              = 60
  evaluation_periods  = 5
  threshold           = 1
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions = {
    LoadBalancer = aws_lb.backend.arn_suffix
    TargetGroup  = aws_lb_target_group.backend.arn_suffix
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

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

resource "aws_cloudwatch_metric_alarm" "rds_latency" {
  for_each            = local.manage_database ? toset(["ReadLatency", "WriteLatency"]) : toset([])
  alarm_name          = "${local.name}-rds-${lower(each.value)}"
  namespace           = "AWS/RDS"
  metric_name         = each.value
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 0.05
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.postgres[0].identifier }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  count               = 2
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

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  count               = 2
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
    CPUUtilization    = 70
    MemoryUtilization = 75
  }
  alarm_name          = "${local.name}-ecs-${lower(each.key)}"
  namespace           = "AWS/ECS"
  metric_name         = each.key
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = each.value
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
  threshold           = var.desired_count
  comparison_operator = "LessThanThreshold"
  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.backend.name
  }
  alarm_actions = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_log_metric_filter" "hikari_timeouts" {
  name           = "${local.name}-hikari-timeouts"
  log_group_name = aws_cloudwatch_log_group.backend.name
  pattern        = "\"Connection is not available, request timed out\""
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

resource "aws_cloudwatch_metric_alarm" "ses_reputation" {
  for_each = {
    "Reputation.BounceRate"    = 0.05
    "Reputation.ComplaintRate" = 0.001
  }
  alarm_name          = "${local.name}-ses-${lower(replace(each.key, ".", "-"))}"
  namespace           = "AWS/SES"
  metric_name         = each.key
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 1
  threshold           = each.value
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_dashboard" "capacity" {
  dashboard_name = "${local.name}-capacity"
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "ECS capacity"
          region = var.aws_region
          stat   = "Average"
          period = 60
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.backend.name],
            [".", "MemoryUtilization", ".", ".", ".", "."],
            ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.backend.name]
          ]
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "ALB latency and errors"
          region = var.aws_region
          period = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.backend.arn_suffix, "TargetGroup", aws_lb_target_group.backend.arn_suffix, { stat = "p95" }],
            [".", "HTTPCode_Target_5XX_Count", ".", ".", ".", ".", { stat = "Sum" }],
            ["JadhavrERP/${var.environment}", "Http429Count", { stat = "Sum" }]
          ]
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          title  = "Redis and queues"
          region = var.aws_region
          period = 60
          metrics = concat(
            [for queue in [aws_sqs_queue.email.name, aws_sqs_queue.report.name] :
            ["AWS/SQS", "ApproximateAgeOfOldestMessage", "QueueName", queue]],
            [for cluster in tolist(aws_elasticache_replication_group.redis.member_clusters) :
            ["AWS/ElastiCache", "Evictions", "CacheClusterId", cluster]]
          )
        }
      },
      {
        type   = "log"
        width  = 12
        height = 6
        properties = {
          title  = "Recent backend errors"
          region = var.aws_region
          query  = "SOURCE '${aws_cloudwatch_log_group.backend.name}' | fields @timestamp, @message | filter @message like /ERROR|timed out/ | sort @timestamp desc | limit 50"
          view   = "table"
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
        Action   = ["s3:DeleteObject", "s3:GetObject", "s3:ListBucket", "s3:PutObject"]
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
      }
      ], local.manage_database ? [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        Resource = "${aws_ecs_task_definition.migration.arn_without_revision}:*"
      }
      ] : [], [
      {
        Effect   = "Allow"
        Action   = ["ecs:UpdateService"]
        Resource = aws_ecs_service.backend.id
      },
      {
        Effect    = "Allow"
        Action    = ["iam:PassRole"]
        Resource  = [aws_iam_role.ecs_execution.arn, aws_iam_role.ecs_task.arn]
        Condition = { StringEquals = { "iam:PassedToService" = "ecs-tasks.amazonaws.com" } }
      },
      {
        Effect   = "Allow"
        Action   = ["cloudfront:CreateInvalidation"]
        Resource = aws_cloudfront_distribution.main.arn
      }
    ])
  })
}
