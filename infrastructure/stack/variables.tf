variable "namespace" {
  type = string
}

variable "catalogue_namespace" {
  type = string
}

variable "logging_cluster_id" {
  type = string
}

variable "network_config" {
  type = object({
    ec_privatelink_security_group_id = string
    ec_traffic_filters               = list(string)
  })
}

variable "aggregator_repository" {
  type = object({
    name = string
    url  = string
  })
}

variable "aggregator_bulk_repository" {
  type = object({
    name = string
    url  = string
  })
}

variable "ingestor_repository" {
  type = object({
    name = string
    url  = string
  })
}

variable "recorder_repository" {
  type = object({
    name = string
    url  = string
  })
}

variable "recorder_bulk_repository" {
  type = object({
    name = string
    url  = string
  })
}
