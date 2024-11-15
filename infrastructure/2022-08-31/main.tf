module "concepts_pipeline" {
  source = "../stack"

  namespace = local.pipeline_date

  # This is the namespace of the works catalogue pipeline
  # that this concept pipeline should subscribe to for changes.
  catalogue_namespace = "2024-08-15" // This is automatically bumped by the catalogue-api repo

  network_config     = local.network_config
  logging_cluster_id = local.logging_cluster_id

  aggregator_repository      = data.terraform_remote_state.concepts_shared.outputs.ecr_repository_concepts_aggregator
  aggregator_bulk_repository = data.terraform_remote_state.concepts_shared.outputs.ecr_repository_concepts_aggregator_bulk
  recorder_repository        = data.terraform_remote_state.concepts_shared.outputs.ecr_repository_concepts_recorder
  recorder_bulk_repository   = data.terraform_remote_state.concepts_shared.outputs.ecr_repository_concepts_recorder_bulk
}
