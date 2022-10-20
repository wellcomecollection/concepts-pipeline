
locals {
  # The lambda event source pulls messages from SQS in batches, finally triggering the lambda
  # when either it has enough messages, or enough time has elapsed.
  # A message becomes invisible when it joins the event source buffer, so could wait for
  # the whole timeout window plus the whole execution time before being confirmed.
  # The value of visibility timeout must be at least 20 seconds more than the lambda timeout
  # This doesn't necessarily need to exist with a longer batching window, but
  # always adding 20 here should mean that you can safely set batching window to 0
  # if you wish.
  # See: https://docs.aws.amazon.com/lambda/latest/dg/with-sqs.html
  # "Lambda might wait for up to 20 seconds before invoking your function."
  queue_visibility_timeout = var.event_batching_window_timeout + var.lambda_timeout + 20
}

module "input_queue" {
  source = "github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.2.1"

  queue_name = "${var.namespace}_${var.service_name}_input"

  topic_arns                 = var.topic_arns
  visibility_timeout_seconds = local.queue_visibility_timeout
  max_receive_count          = 1

  # This is a pretty arbitrary number set fairly small during this early stage
  # so that we don't end up with loads of old messages on the queue if we switch things off,
  # but large enough that we can disable things and reinstate them and stillhave messages to process.
  # Work out what this number should really be, and whether it should be a variable.
  message_retention_seconds  = 1200
  alarm_topic_arn            = ""
}

resource "aws_iam_role_policy_attachment" "lambda_sqs_role_policy" {
  role       = var.lambda_role_name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

resource "aws_lambda_event_source_mapping" "event_source_mapping" {
  event_source_arn                   = module.input_queue.arn
  function_name                      = var.lambda_function_arn
  maximum_batching_window_in_seconds = var.event_batching_window_timeout
  batch_size                         = var.batch_size
}
