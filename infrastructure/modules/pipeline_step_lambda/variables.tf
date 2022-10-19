
variable "ecr_repository" {
  type = object({
    name = string
    url  = string
  })
  description = "The ECR repository where the image for the function is stored"
}

variable "namespace" {
  type = string
}

variable "service_name" {
  type        = string
  description = "A name for this step"
}

variable "elasticsearch_host_secret" {
  type = object({
    name = string
    arn  = string
  })
  description = "name and arn of a SecretsManager secret, containing the Elasticsearch Host to write to"
}

variable "elasticsearch_user" {
  type = object({
    password_secret_name = string
    password_secret_arn  = string
  })
  description = "name and arn of a SecretsManager secret, containing the Elasticsearch user this step should use to authenticate"
}

variable "environment_variables" {
  type        = map(string)
  description = "Arbitrary environment variables to give to the Lambda"
  default     = {}
}

