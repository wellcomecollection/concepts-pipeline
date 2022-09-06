locals {
  elastic_cloud_region = "eu-west-1"

  cluster_id           = ec_deployment.concepts.elasticsearch[0].resource_id
  cluster_alias        = ec_deployment.concepts.alias
  cluster_public_host  = "${local.cluster_alias}.es.${local.elastic_cloud_region}.aws.found.io"
  cluster_private_host = "${local.cluster_id}.vpce.${local.elastic_cloud_region}.aws.elastic-cloud.com"
}

data "ec_stack" "latest_patch" {
  version_regex = "8.4.?"
  region        = local.elastic_cloud_region
}

resource "ec_deployment" "concepts" {
  name    = "concepts-${var.namespace}"
  alias   = "concepts-${var.namespace}"
  version = data.ec_stack.latest_patch.version

  traffic_filter         = var.network_config.ec_traffic_filters
  deployment_template_id = "aws-io-optimized-v2"
  region                 = local.elastic_cloud_region

  elasticsearch {
    topology {
      id         = "hot_content"
      zone_count = 1
      size       = "2g"
    }
  }

  kibana {
    topology {
      zone_count = 1
      size       = "1g"
    }
  }

  observability {
    deployment_id = var.logging_cluster_id
  }
}

module "host_secrets" {
  source = "github.com/wellcomecollection/terraform-aws-secrets?ref=v1.4.0"

  key_value_map = {
    "elasticsearch/${ec_deployment.concepts.name}/public_host"  = local.cluster_public_host
    "elasticsearch/${ec_deployment.concepts.name}/private_host" = local.cluster_private_host
  }
}
