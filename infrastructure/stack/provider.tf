terraform {
  required_providers {
    ec = {
      source = "elastic/ec"
    }
    elasticstack = {
      source = "elastic/elasticstack"
    }
  }
}

provider "elasticstack" {
  elasticsearch {
    username  = ec_deployment.concepts.elasticsearch_username
    password  = ec_deployment.concepts.elasticsearch_password
    endpoints = ec_deployment.concepts.elasticsearch.*.https_endpoint
  }
}
