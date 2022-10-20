variable "namespace" {
  type = string
}

variable "service_name" {
  type = string
}

variable "topic_arns" {
  type = list(string)
  default = []
}

variable "lambda_function_arn" {
  type = string
}

variable "lambda_role_name" {
  type = string
}

variable "lambda_timeout" {
  type = number
}

variable "event_batching_window_timeout" {
  type = number
  default = 20
}

variable "batch_size" {
  type = number
  default = 10
}