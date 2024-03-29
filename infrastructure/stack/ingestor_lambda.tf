#
# The Concepts ingestor Lambda function
#
# This manages the lambda function for the concepts ingestor.
# The function fetches sets of concepts from an Authority,
# extracts the entries as Concepts, and stores them in Elasticsearch

module "ingestor_lambda" {
  source = "../modules/pipeline_step_lambda"

  ecr_repository = var.ingestor_repository
  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user = module.client_service_users["ingestor"]
  namespace          = var.namespace
  service_name       = "ingestor"

  # The ingestor only runs in bulk mode, which takes some time
  # 900s (15 minutes) is the maximum lambda execution time allowed
  timeout = 900

  environment_variables = {
    index_name = local.elastic_indices.authoritative-concepts
  }
}
