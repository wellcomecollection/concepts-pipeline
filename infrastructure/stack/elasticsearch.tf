locals {
  elastic_cloud_region = "eu-west-1"
}

data "ec_stack" "latest_patch" {
  version_regex = "8.4.?"
  region        = local.elastic_cloud_region
}

resource "ec_deployment" "concepts" {
  name    = "concepts-${var.namespace}"
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
