#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
module "aggregator_lambda" {
  source = "../modules/pipeline_step_lambda"

  ecr_repository                = var.aggregator_repository
  elasticsearch_host_secret     = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user            = module.client_service_users["aggregator"]
  namespace                     = var.namespace
  service_name                  = "concepts_aggregator"
}