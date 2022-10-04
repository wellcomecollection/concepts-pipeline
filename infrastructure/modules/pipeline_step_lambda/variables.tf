
variable "ecr_repository" {
  type = object({
    name = string
    url  = string
  })
}

variable "namespace" {
  type = string
}

variable "service_name" {
  type = string
}

variable "elasticsearch_host_secret" {
  type = object({
    name = string
    arn  = string
  })
}

variable "elasticsearch_user" {
  type = object({
    password_secret_name = string
    password_secret_arn  = string
  })
}