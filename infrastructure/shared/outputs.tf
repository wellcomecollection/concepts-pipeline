output "ecr_repository_concepts_aggregator" {
  value = {
    name = aws_ecr_repository.concepts_aggregator.name
    url  = aws_ecr_repository.concepts_aggregator.repository_url
  }
}

output "ecr_repository_concepts_aggregator_bulk" {
  value = {
    name = aws_ecr_repository.concepts_aggregator_bulk.name
    url  = aws_ecr_repository.concepts_aggregator_bulk.repository_url
  }
}

output "ecr_repository_concepts_recorder" {
  value = {
    name = aws_ecr_repository.concepts_recorder.name
    url  = aws_ecr_repository.concepts_recorder.repository_url
  }
}

output "ecr_repository_concepts_recorder_bulk" {
  value = {
    name = aws_ecr_repository.concepts_recorder_bulk.name
    url  = aws_ecr_repository.concepts_recorder_bulk.repository_url
  }
}
