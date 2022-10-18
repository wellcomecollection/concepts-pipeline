
output "lambda_role" {
  value = aws_iam_role.lambda_role
}

output "lambda_function" {
  value = aws_lambda_function.pipeline_step
}