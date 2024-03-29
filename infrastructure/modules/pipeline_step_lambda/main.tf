
locals {
  ecr_image_tag     = var.namespace
  service_full_name = "concepts_${var.service_name}"
}


data "aws_ecr_image" "lambda_image" {
  repository_name = var.ecr_repository.name
  image_tag       = local.ecr_image_tag
}

module "pipeline_step" {
  source = "github.com/wellcomecollection/terraform-aws-lambda.git?ref=v1.1.1"

  name         = "${var.namespace}-${local.service_full_name}"
  package_type = "Image"
  image_uri    = "${var.ecr_repository.url}@${data.aws_ecr_image.lambda_image.id}"
  timeout      = var.timeout
  memory_size  = var.memory_size
  description  = var.description
  # No Concurrency:
  # Pipeline steps invoked on a schedule or manually are expected to be run very infrequently,
  # so will not overlap.
  # Pipeline steps invoked (directly or indirectly) by the works ingestor will have a low,
  # but bursty throughput.
  # Setting concurrency to 1 speeds things up by preventing cold starts,
  # which have an overhead of about 13 seconds, partly due to secret lookups.
  # Taking the Aggregator as an example and given a batch size of 10
  # (default for Lamdba SQS triggers):
  # Running a warm function takes about 300 ms.
  # So running once takes ca. 13.3ms, twice takes 13.6s
  # If we allow parallel execution, then, in a worst case scenario,
  # where there are 19 messages to process, and a 20th arrives at
  # about 13000ms after the first function is invoked, it could take
  # over 26 seconds to process all 20.
  reserved_concurrent_executions = 1

  environment = {
    variables = merge({
      "${upper(var.service_name)}_APP_CONTEXT" = "remote"
      es_host                                  = var.elasticsearch_host_secret.name
      es_password                              = var.elasticsearch_user.password_secret_name
    }, var.environment_variables)
  }
}

data "aws_iam_policy_document" "secrets_access" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue"
    ]
    effect = "Allow"
    resources = [
      var.elasticsearch_host_secret.arn,
      var.elasticsearch_user.password_secret_arn
    ]
  }
}


resource "aws_iam_policy" "secrets_access" {
  name   = "${var.namespace}-${local.service_full_name}-secrets-access"
  policy = data.aws_iam_policy_document.secrets_access.json
}

resource "aws_iam_role_policy_attachment" "lambda_role_attachment" {
  role       = module.pipeline_step.lambda_role.name
  policy_arn = aws_iam_policy.secrets_access.arn
}
