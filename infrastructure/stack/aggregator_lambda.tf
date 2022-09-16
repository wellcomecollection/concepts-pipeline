locals {
  ecr_image_tag   = "latest" # for now.  Once we make it work like weco-deploy, use the namespace
  service_name    = "concepts_aggregator"
}

resource "aws_iam_role" "concepts_aggregator_role" {
  name = "concepts_aggregator_role"


  assume_role_policy = ""
}

data aws_ecr_image lambda_image {
  repository_name = var.aggregator_repository.name
  image_tag       = local.ecr_image_tag
}
#
#resource aws_iam_policy lambda {
#  name = "${local.prefix}-lambda-policy"
#  path = "/"
#  policy =
#}

resource "aws_lambda_function" "concepts_aggregator" {
  function_name = "${var.namespace}-concepts_aggregator"
  handler       = "weco.concepts.aggregator.LambdaMain::handleRequest"
  package_type = "Image"
  image_uri = "${var.aggregator_repository.url}@${data.aws_ecr_image.lambda_image.id}"
  role = aws_iam_role.concepts_aggregator_role.arn
}