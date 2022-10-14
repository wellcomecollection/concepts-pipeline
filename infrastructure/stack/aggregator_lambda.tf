#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
locals {
  catalogue_account = 760097843905
  works_ingestor_topic = "arn:aws:sns:eu-west-1:${local.catalogue_account}:catalogue-${var.catalogue_namespace}_ingestor_works_output"
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
}

module "input_queue" {
  source = "github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.2.1"

  queue_name = "${var.namespace}_aggregator_input"

  topic_arns                 = [local.works_ingestor_topic]
  visibility_timeout_seconds = 10
  max_receive_count          = 1
  message_retention_seconds  = 1200
  alarm_topic_arn = ""
}

