locals {
  password_secret = "elasticsearch/${var.cluster_name}/${var.username}/password"
}

resource "random_password" "password" {
  length = 64
}

resource "elasticstack_elasticsearch_security_user" "user" {
  username = var.username
  # ES uses bcrypt by default https://www.elastic.co/guide/en/elasticsearch/reference/current/security-settings.html#password-hashing-algorithms
  password_hash = random_password.password.bcrypt_hash
  roles         = var.roles
}

module "secrets" {
  source = "github.com/wellcomecollection/terraform-aws-secrets?ref=v1.4.0"

  deletion_mode = "IMMEDIATE"
  key_value_map = {
    "${local.password_secret}" = random_password.password.result
  }
}
