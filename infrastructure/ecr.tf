resource "aws_ecr_repository" "concepts_ingestor" {
  name = "weco/concepts_ingestor"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "concepts_aggregator" {
  name = "weco/concepts_aggregator"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "concepts_aggregator_bulk" {
  name = "weco/concepts_aggregator_bulk"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_repository" "concepts_recorder" {
  name = "weco/concepts_recorder"

  lifecycle {
    prevent_destroy = true
  }
}


resource "aws_ecr_repository" "concepts_recorder_bulk" {
  name = "weco/concepts_recorder_bulk"

  lifecycle {
    prevent_destroy = true
  }
}