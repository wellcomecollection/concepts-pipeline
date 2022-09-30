#
# The Concepts Aggregator Lambda function
#
# This manages the lambda function for the concepts aggregator.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
locals {
  ecr_image_tag = var.namespace
  service_name  = "concepts_aggregator"
  secrets = {
    elasticsearch_host = {
      name = "elasticsearch/concepts-${var.namespace}/public_host"
      arn  = module.host_secrets.arns[1]
    }
    elasticsearch_password = {
      name = module.client_service_users["aggregator"].password_secret_name
      arn  = module.client_service_users["aggregator"].password_secret_arn
    }
  }
}

data "aws_ecr_image" "lambda_image" {
  repository_name = var.aggregator_repository.name
  image_tag       = local.ecr_image_tag
}


data "aws_iam_policy_document" "concepts_aggregator_permissions" {
  statement {
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]
    effect    = "Allow"
    resources = ["*"]
    sid       = "CreateCloudWatchLogs"
  }
  statement {
    actions = [
      "secretsmanager:GetSecretValue"
    ]
    effect = "Allow"
    resources = [
      local.secrets.elasticsearch_host.arn,
      local.secrets.elasticsearch_password.arn
    ]
  }
}


resource "aws_iam_role" "concepts_aggregator_role" {
  name               = "${var.namespace}-concepts_aggregator_role"
  assume_role_policy = <<EOF
{
   "Version": "2012-10-17",
   "Statement": [
       {
           "Action": "sts:AssumeRole",
           "Principal": {
               "Service": "lambda.amazonaws.com"
           },
           "Effect": "Allow"
       }
   ]
}
 EOF
}


resource "aws_iam_policy" "concepts_aggregator_policy" {
  name   = "${var.namespace}-lambda-policy"
  policy = data.aws_iam_policy_document.concepts_aggregator_permissions.json
}

resource "aws_iam_role_policy_attachment" "concepts_aggregator_attachment" {
  role       = aws_iam_role.concepts_aggregator_role.name
  policy_arn = aws_iam_policy.concepts_aggregator_policy.arn
}

resource "aws_lambda_function" "concepts_aggregator" {
  function_name = "${var.namespace}-concepts_aggregator"
  package_type  = "Image"
  image_uri     = "${var.aggregator_repository.url}@${data.aws_ecr_image.lambda_image.id}"
  timeout       = 90
  memory_size   = 1024
  environment {
    variables = {
      AGGREGATOR_APP_CONTEXT = "remote"
      es_host                = local.secrets.elasticsearch_host.name
      es_password            = local.secrets.elasticsearch_password.name
    }
  }
  role = aws_iam_role.concepts_aggregator_role.arn
}
