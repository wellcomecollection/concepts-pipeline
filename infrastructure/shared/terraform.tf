terraform {
  backend "s3" {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"

    bucket         = "wellcomecollection-catalogue-infra-delta"
    key            = "terraform/concepts-pipeline/shared.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }

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
