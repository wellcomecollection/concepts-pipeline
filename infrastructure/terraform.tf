terraform {
  backend "s3" {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"

    bucket         = "wellcomecollection-catalogue-infra-delta"
    key            = "terraform/concepts-pipeline/concepts_pipeline.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.22.0"
    }
    ec = {
      source  = "elastic/ec"
      version = ">= 0.4.1"
    }
  }
}

provider "aws" {
  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }

  default_tags {
    tags = {
      TerraformConfigurationURL = "https://github.com/wellcomecollection/concepts-pipeline/tree/main/infrastructure"
      Department                = "Digital Platform"
    }
  }
}

provider "ec" {}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::760097843905:role/platform-read_only"
    bucket   = "wellcomecollection-platform-infra"
    key      = "terraform/platform-infrastructure/shared.tfstate"
    region   = "eu-west-1"
  }
}
