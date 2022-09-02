locals {
  service_roles = {
    ingestor   = ["authoritative-concepts_read", "authoritative-concepts_write"]
    aggregator = ["concepts-used_read", "concepts-used_write"]
  }
  all_roles = toset(flatten(values(local.service_roles)))
}

resource "elasticstack_elasticsearch_security_role" "index_roles" {
  for_each = local.all_roles

  name = each.key
  indices {
    names      = [split("_", each.key)[0]]
    privileges = [split("_", each.key)[1]]
  }
}

module "client_service_users" {
  source   = "../modules/elastic_user"
  for_each = local.service_roles

  username     = each.key
  cluster_name = ec_deployment.concepts.name
  roles        = each.value
}
