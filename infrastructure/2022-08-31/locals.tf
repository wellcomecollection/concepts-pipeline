locals {
  shared_infra       = data.terraform_remote_state.shared_infra.outputs
  logging_cluster_id = local.shared_infra["logging_cluster_id"]

  network_config = {
    ec_traffic_filters = [
      local.shared_infra["ec_catalogue_privatelink_traffic_filter_id"],
      local.shared_infra["ec_public_internet_traffic_filter_id"],
    ]
    # This is not yet used but will be necessary and is easy to forget
    ec_privatelink_security_group_id = local.shared_infra["ec_catalogue_privatelink_sg_id"],
  }
}
