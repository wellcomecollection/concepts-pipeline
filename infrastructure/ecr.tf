resource "aws_ecr_repository" "concepts_ingestor" {
  name = "weco/concepts_ingestor"

  lifecycle {
    prevent_destroy = true
  }
}
