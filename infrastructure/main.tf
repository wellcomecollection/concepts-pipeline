module "concepts_pipeline" {
  source = "./stack"

  namespace = "2022-08-31"

  network_config     = local.network_config
  logging_cluster_id = local.logging_cluster_id
}
