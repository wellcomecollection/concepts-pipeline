module "recorder_lambda" {
  source = "../modules/pipeline_step_lambda"

  ecr_repository = var.recorder_repository
  elasticsearch_host_secret = {
    name = "elasticsearch/concepts-${var.namespace}/public_host"
    arn  = module.host_secrets.arns[1]
  }
  elasticsearch_user = module.client_service_users["recorder"]
  namespace          = var.namespace
  service_name       = "recorder"

  environment_variables = {
    catalogue_concepts_index     = local.elastic_indices.catalogue-concepts
    authoritative_concepts_index = local.elastic_indices.authoritative-concepts
    concepts_store_index         = local.elastic_indices.concepts-store
  }
}

module "recorder_input_queue" {
  source = "../modules/lambda_input_queue"

  lambda_function_arn = module.recorder_lambda.lambda_function.arn
  lambda_role_name    = module.recorder_lambda.lambda_role.name
  lambda_timeout      = module.recorder_lambda.lambda_function.timeout
  namespace           = var.namespace
  service_name        = "recorder"
  topic_arns          = [module.updates_topic.arn]
}
