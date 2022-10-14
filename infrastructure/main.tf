module "concepts_pipeline" {
  source = "./stack"

  namespace = "2022-08-31"
  catalogue_namespace = "2022-10-03"

  network_config     = local.network_config
  logging_cluster_id = local.logging_cluster_id

  aggregator_repository = {
    name = aws_ecr_repository.concepts_aggregator.name
    url  = aws_ecr_repository.concepts_aggregator.repository_url
  }

  ingestor_repository = {
    name = aws_ecr_repository.concepts_ingestor.name
    url  = aws_ecr_repository.concepts_ingestor.repository_url
  }
}
