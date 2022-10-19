#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
module "aggregator_lambda" {
  source = "../modules/pipeline_step_lambda"

  service_name   = "aggregator"
  ecr_repository = var.aggregator_repository
  namespace      = var.namespace

  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user = module.client_service_users["aggregator"]

  environment_variables = {
    updates_topic = module.updates_topic.arn
  }
}

module "updates_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.1"

  name = "catalogue-concept-updates"
}
