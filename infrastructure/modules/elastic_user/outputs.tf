output "password_secret_name" {
  value = local.password_secret
}

output "password_secret_arn" {
  value = module.secrets.arns[0]
}
