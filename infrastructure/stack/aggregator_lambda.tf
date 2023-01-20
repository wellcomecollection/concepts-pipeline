#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
locals {
  works_ingestor_output_topic = "arn:aws:sns:eu-west-1:${local.catalogue_account}:catalogue-${var.catalogue_namespace}_ingestor_works_output"
  # This function takes 13-15 seconds to start up, and ca. 300ms to run over 10 records
  # A timeout of 20 seconds gives plenty of buffer if we choose to run it over more
  # records, or if any service it relies on is uncharacteristically slow,
  # without excessively permitting it to run for ages.
  lambda_timeout                = 20
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

  # Timeout and memory settings for bulk mode.
  # This is for manually running the ingest of the
  lambda_bulk_timeout     = 600
  lambda_bulk_memory_size = 2048

}

module "aggregator_lambda" {
  source = "../modules/pipeline_step_lambda"

  service_name   = "aggregator"
  ecr_repository = var.aggregator_repository
  namespace      = var.namespace
  description    = "Aggregate concepts used in Works when they are ingested by the works pipeline"
  timeout        = local.lambda_timeout

  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user = module.client_service_users["aggregator"]

  environment_variables = {
    index_name       = local.elastic_indices.catalogue-concepts
    updates_topic    = module.updates_topic.arn
    workurl_template = "https://api.wellcomecollection.org/catalogue/v2/works/%s?include=identifiers,subjects,contributors,genres"

  }
}

module "aggregator_bulk_lambda" {
  # This is a detached, high-performance/long-timeout version of the aggregator.
  # This is intended to be triggered manually for pulling in all the works from
  # the Works API Snapshot
  source = "../modules/pipeline_step_lambda"

  service_name   = "aggregator_bulk"
  ecr_repository = var.aggregator_bulk_repository
  namespace      = var.namespace
  description    = "Aggregate concepts used in Works when they are ingested by the works snapshot"
  timeout        = local.lambda_bulk_timeout
  memory_size    = local.lambda_bulk_memory_size

  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }

  elasticsearch_user = module.client_service_users["aggregator"]

  environment_variables = {
    index_name = local.elastic_indices.catalogue-concepts
  }
}

module "aggregator_input_queue" {
  source = "../modules/lambda_input_queue"

  lambda_function_arn = module.aggregator_lambda.lambda_function.arn
  lambda_role_name    = module.aggregator_lambda.lambda_role.name
  lambda_timeout      = module.aggregator_lambda.lambda_function.timeout
  namespace           = var.namespace
  service_name        = "aggregator"
  topic_arns          = [local.works_ingestor_output_topic]
}


module "updates_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.1"

  name = "catalogue-concept-updates"
}

resource "aws_iam_policy" "publish_to_updates_topic" {
  name        = "${var.namespace}-publish-to-updates-topic"
  description = "Allows publication of notifications to the concept updates topic"

  policy = module.updates_topic.publish_policy
}

resource "aws_iam_role_policy_attachment" "publish_to_updates_topic" {
  role       = module.aggregator_lambda.lambda_role.name
  policy_arn = aws_iam_policy.publish_to_updates_topic.arn
}
