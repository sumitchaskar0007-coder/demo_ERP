resource "aws_sqs_queue" "email_dlq" {
  name                      = "${local.name}-email-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "email" {
  name                       = "${local.name}-email"
  visibility_timeout_seconds = 120
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  sqs_managed_sse_enabled    = true
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.email_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue" "report_dlq" {
  name                      = "${local.name}-report-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "report" {
  name                       = "${local.name}-report"
  visibility_timeout_seconds = 900
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 20
  sqs_managed_sse_enabled    = true
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.report_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_iam_role_policy" "ecs_async_queues" {
  name = "async-job-queues"
  role = aws_iam_role.ecs_task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:ChangeMessageVisibility",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes",
        "sqs:GetQueueUrl",
        "sqs:ReceiveMessage",
        "sqs:SendMessage"
      ]
      Resource = [aws_sqs_queue.email.arn, aws_sqs_queue.report.arn]
    }]
  })
}

resource "aws_cloudwatch_metric_alarm" "async_queue_oldest" {
  for_each = {
    email  = aws_sqs_queue.email.name
    report = aws_sqs_queue.report.name
  }
  alarm_name          = "${local.name}-${each.key}-queue-oldest"
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateAgeOfOldestMessage"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 5
  threshold           = each.key == "email" ? 300 : 900
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { QueueName = each.value }
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "async_dlq_messages" {
  for_each = {
    email  = aws_sqs_queue.email_dlq.name
    report = aws_sqs_queue.report_dlq.name
  }
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
