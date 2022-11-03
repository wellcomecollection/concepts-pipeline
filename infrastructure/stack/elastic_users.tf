locals {
  indices = ["authoritative-concepts", "catalogue-concepts", "concepts-store"]
  service_roles = {
    ingestor   = ["authoritative-concepts_read", "authoritative-concepts_write"]
    aggregator = ["catalogue-concepts_read", "catalogue-concepts_write"]
    recorder   = ["catalogue-concepts_read", "authoritative-concepts_read", "concepts-store_write"]
  }
}

resource "elasticstack_elasticsearch_security_role" "read_indices" {
  for_each = toset(local.indices)

  name = "${each.key}_read"
  indices {
    names      = ["${each.key}*"]
    privileges = ["read", "monitor", "view_index_metadata"]
  }
}

resource "elasticstack_elasticsearch_security_role" "write_indices" {
  for_each = toset(local.indices)

  name = "${each.key}_write"
  indices {
    names = ["${each.key}*"]
    // See https://www.elastic.co/guide/en/elasticsearch/reference/current/security-privileges.html#privileges-list-indices for details
    // This doesn't allow index deletion (but does allow document deletion)
    privileges = ["create_index", "index", "create", "delete"]
  }
}

module "client_service_users" {
  source   = "../modules/elastic_user"
  for_each = local.service_roles

  username     = each.key
  cluster_name = ec_deployment.concepts.name
  roles        = each.value
}
