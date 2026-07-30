# Review-only queue infrastructure. The application and dedicated ECS worker
# are wired below, but async_queues_enabled must remain false until the staging
# plan/cost, database migrations, alert recipient, and SES production access
# are approved. Queue messages contain only durable database IDs.

resource "aws_sqs_queue" "email_dlq" {
  count                     = var.async_queues_enabled ? 1 : 0
  name                      = "${local.name}-email-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_sqs_queue" "email" {
  count                      = var.async_queues_enabled ? 1 : 0
  name                       = "${local.name}-email"
  visibility_timeout_seconds = 120
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  sqs_managed_sse_enabled    = true
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.email_dlq[0].arn
    maxReceiveCount     = 5
  })

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_sqs_queue_redrive_allow_policy" "email" {
  count     = var.async_queues_enabled ? 1 : 0
  queue_url = aws_sqs_queue.email_dlq[0].id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.email[0].arn]
  })
}

resource "aws_sqs_queue" "report_dlq" {
  count                     = var.async_queues_enabled ? 1 : 0
  name                      = "${local.name}-report-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_sqs_queue" "report" {
  count                      = var.async_queues_enabled ? 1 : 0
  name                       = "${local.name}-report"
  visibility_timeout_seconds = 900
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  sqs_managed_sse_enabled    = true
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.report_dlq[0].arn
    maxReceiveCount     = 3
  })

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_sqs_queue_redrive_allow_policy" "report" {
  count     = var.async_queues_enabled ? 1 : 0
  queue_url = aws_sqs_queue.report_dlq[0].id
  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.report[0].arn]
  })
}

resource "aws_iam_role_policy" "ecs_api_async_queues" {
  count = var.async_queues_enabled ? 1 : 0
  name  = "publish-async-jobs"
  role  = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [{
        Sid      = "PublishReportJobs"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage"]
        Resource = aws_sqs_queue.report[0].arn
      }],
      var.mail_enabled ? [{
        Sid      = "PublishEmailJobs"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage"]
        Resource = aws_sqs_queue.email[0].arn
      }] : []
    )
  })
}

resource "aws_iam_role" "async_worker" {
  count = var.async_queues_enabled ? 1 : 0
  name  = "${local.name}-async-worker"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "async_worker_queues" {
  count = var.async_queues_enabled ? 1 : 0
  name  = "consume-async-jobs"
  role  = aws_iam_role.async_worker[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [{
        Sid    = "ConsumeReportJobs"
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage"
        ]
        Resource = aws_sqs_queue.report[0].arn
      }],
      var.mail_enabled ? [{
        Sid    = "PublishAndConsumeEmailJobs"
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:ChangeMessageVisibility"
        ]
        Resource = aws_sqs_queue.email[0].arn
      }] : []
    )
  })
}

resource "aws_iam_role_policy" "async_worker_storage" {
  count = var.async_queues_enabled ? 1 : 0
  name  = "private-report-objects"
  role  = aws_iam_role.async_worker[0].id

  # Report keys are generated below a server-enforced tenant/job prefix. Future
  # tenant UUIDs cannot be enumerated in an S3 object IAM resource.
  #tfsec:ignore:aws-iam-no-policy-wildcards
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "ReportObjects"
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.uploads.arn}/*"
      },
      {
        Sid      = "ReportBucket"
        Effect   = "Allow"
        Action   = ["s3:GetBucketLocation", "s3:ListBucket"]
        Resource = aws_s3_bucket.uploads.arn
      },
      {
        Sid    = "ReportObjectEncryption"
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

locals {
  async_queue_alarm_config = var.async_queues_enabled ? {
    email = {
      queue_name       = aws_sqs_queue.email[0].name
      depth_threshold  = var.email_queue_depth_alarm_threshold
      oldest_threshold = var.email_queue_oldest_alarm_seconds
    }
    report = {
      queue_name       = aws_sqs_queue.report[0].name
      depth_threshold  = var.report_queue_depth_alarm_threshold
      oldest_threshold = var.report_queue_oldest_alarm_seconds
    }
  } : {}

  async_dlq_alarm_config = var.async_queues_enabled ? {
    email  = aws_sqs_queue.email_dlq[0].name
    report = aws_sqs_queue.report_dlq[0].name
  } : {}
}

resource "aws_cloudwatch_metric_alarm" "async_queue_depth" {
  for_each            = local.async_queue_alarm_config
  alarm_name          = "${local.name}-${each.key}-queue-depth"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 5
  threshold           = each.value.depth_threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = each.value.queue_name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "async_queue_oldest" {
  for_each            = local.async_queue_alarm_config
  alarm_name          = "${local.name}-${each.key}-queue-oldest"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateAgeOfOldestMessage"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 5
  threshold           = each.value.oldest_threshold
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = each.value.queue_name }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "async_dlq_messages" {
  for_each            = local.async_dlq_alarm_config
  alarm_name          = "${local.name}-${each.key}-dlq-messages"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = each.value }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}
