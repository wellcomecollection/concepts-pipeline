locals {
  ecr_image_tag       = "latest"
}

resource "aws_iam_role" "concepts_aggregator_role" {
  name = "concepts_aggregator_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

data aws_ecr_image lambda_image {
  repository_name = aws_ecr_repository.concepts_aggregator.name
  image_tag       = local.ecr_image_tag
}

resource "aws_lambda_function" "concepts_aggregator" {

  function_name = "concepts_aggregator"
  handler       = "weco.concepts.aggregator.LambdaMain::handleRequest"
  package_type = "Image"
  image_uri = "${aws_ecr_repository.concepts_aggregator.repository_url}@${data.aws_ecr_image.lambda_image.id}"
  role = aws_iam_role.concepts_aggregator_role.arn
}