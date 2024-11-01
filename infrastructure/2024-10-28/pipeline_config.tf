# Note: this file is autogenerated by the run_terraform.sh script.
#
# Edits to this file may be reverted!

locals {
  pipeline_date = "2024-10-28"
}

terraform {
  backend "s3" {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"

    bucket         = "wellcomecollection-catalogue-infra-delta"
    key            = "terraform/concepts-pipeline/2024-10-28.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
