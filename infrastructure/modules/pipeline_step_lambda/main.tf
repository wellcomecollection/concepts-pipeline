#
# The Concepts ingestor Lambda function
#
# This manages the lambda function for the concepts ingestor.
# The function fetches Works from the Catalogue API and stores any
# used concepts in Elasticsearch.
#
locals {
  ecr_image_tag = var.namespace
}


data "aws_ecr_image" "lambda_image" {
  repository_name = var.ecr_repository.name
  image_tag       = local.ecr_image_tag
}


data "aws_iam_policy_document" "concepts_ingestor_permissions" {
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
      var.elasticsearch_host_secret.arn,
      var.elasticsearch_user.password_secret_name
    ]
  }
}


resource "aws_iam_role" "lambda_role" {
  name               = "${var.namespace}-${var.service_name}_role"
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


resource "aws_iam_policy" "concepts_ingestor_policy" {
  name   = "${var.namespace}-lambda-policy"
  policy = data.aws_iam_policy_document.concepts_ingestor_permissions.json
}

resource "aws_iam_role_policy_attachment" "lambda_role_attachment" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = aws_iam_policy.concepts_ingestor_policy.arn
}

resource "aws_lambda_function" "concepts_ingestor" {
  function_name = "${var.namespace}-${var.service_name}"
  package_type  = "Image"
  image_uri     = "${var.ecr_repository.url}@${data.aws_ecr_image.lambda_image.id}"
  timeout       = 600
  memory_size   = 1024
  environment {
    variables = {
      ingestor_APP_CONTEXT = "remote"
      es_host              = var.elasticsearch_host_secret.name
      es_password          = var.elasticsearch_user.password_secret_name
    }
  }
  role = aws_iam_role.lambda_role.arn
}
