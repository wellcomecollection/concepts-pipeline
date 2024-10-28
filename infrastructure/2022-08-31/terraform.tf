terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 4.44.0"
    }
    ec = {
      source  = "elastic/ec"
      version = "0.5.1"
    }
    elasticstack = {
      source  = "elastic/elasticstack"
      version = ">= 0.3.3"
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

provider "elasticstack" {}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::760097843905:role/platform-read_only"
    bucket   = "wellcomecollection-platform-infra"
    key      = "terraform/platform-infrastructure/shared.tfstate"
    region   = "eu-west-1"
  }
}

data "terraform_remote_state" "concepts_shared" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
    bucket   = "wellcomecollection-catalogue-infra-delta"
    key      = "terraform/concepts-pipeline/shared.tfstate"
    region   = "eu-west-1"
  }
}
