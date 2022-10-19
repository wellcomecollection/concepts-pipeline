#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
locals {
  catalogue_account    = 760097843905
  works_ingestor_output_topic = "arn:aws:sns:eu-west-1:${local.catalogue_account}:catalogue-${var.catalogue_namespace}_ingestor_works_output"
  # This function takes 13-15 seconds to start up, and ca. 300ms to run over 10 records
  # A timeout of 20 seconds gives plenty of buffer if we choose to run it over more
  # records, or if any service it relies on is uncharacteristically slow,
  # without excessively permitting it to run for ages.
  lambda_timeout = 20
  event_batching_window_timeout = 60

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
  queue_visibility_timeout = local.lambda_timeout + local.event_batching_window_timeout + 20
}

module "aggregator_lambda" {
  source = "../modules/pipeline_step_lambda"

  ecr_repository = var.aggregator_repository
  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user = module.client_service_users["aggregator"]
  namespace          = var.namespace
  service_name       = "aggregator"
  description        = "Aggregate concepts used in Works when they are ingested by the works pipeline"
  timeout = local.lambda_timeout
}

module "input_queue" {
  source = "github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.2.1"

  queue_name = "${var.namespace}_aggregator_input"

  topic_arns                 = [local.works_ingestor_output_topic]
  visibility_timeout_seconds = local.queue_visibility_timeout
  max_receive_count          = 1
  message_retention_seconds  = 1200
  alarm_topic_arn            = ""
}

resource "aws_iam_role_policy_attachment" "lambda_sqs_role_policy" {
  role       = module.aggregator_lambda.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

resource "aws_lambda_event_source_mapping" "event_source_mapping" {
  event_source_arn = module.input_queue.arn
  function_name    = module.aggregator_lambda.lambda_function.arn
  maximum_batching_window_in_seconds = local.event_batching_window_timeout
}
