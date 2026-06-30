terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

locals {
  domains = ["order", "member", "product", "delivery"]

  visibility_timeout_seconds = 60
  message_retention_seconds  = 1209600 # 14 days (main queues)
  dlq_retention_seconds      = 1209600 # 14 days (DLQ)
  max_receive_count          = 5
}

resource "aws_sqs_queue" "dlq" {
  for_each = toset(local.domains)

  name                      = "${each.value}-changed-dlq"
  message_retention_seconds = local.dlq_retention_seconds

  tags = {
    Service = "query-service"
    Domain  = each.value
    Kind    = "dlq"
  }
}

resource "aws_sqs_queue" "main" {
  for_each = toset(local.domains)

  name                       = "${each.value}-changed"
  visibility_timeout_seconds = local.visibility_timeout_seconds
  message_retention_seconds  = local.message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.value].arn
    maxReceiveCount     = local.max_receive_count
  })

  tags = {
    Service = "query-service"
    Domain  = each.value
    Kind    = "main"
  }
}

resource "aws_cloudwatch_metric_alarm" "dlq_not_empty" {
  for_each = toset(local.domains)

  alarm_name          = "${each.value}-changed-dlq-not-empty"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.dlq[each.value].name
  }

  alarm_description = "DLQ ${each.value}-changed-dlq has messages — investigate poison events."
}

output "queue_urls" {
  value = { for k, q in aws_sqs_queue.main : k => q.url }
}

output "dlq_urls" {
  value = { for k, q in aws_sqs_queue.dlq : k => q.url }
}
