#!/usr/bin/env bash
set -o errexit
set -o nounset

# Get the path to the current directory, which we can use to find the
# 'scripts' folder and the date of the current pipeline.
#
# https://stackoverflow.com/q/59895/1558022
THIS_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

ROOT=$(git rev-parse --show-toplevel)
. $ROOT/scripts/is_up_to_date.sh

# Create the config file that tells Terraform which pipeline we're running
# in and where to store the remote state.
export PIPELINE_DATE="$(basename "$THIS_DIR")"
. $ROOT/scripts/create_terraform_config_file.sh


AWS_CLI_PROFILE="concepts-pipeline-terraform"
CATALOGUE_DEVELOPER_ARN="arn:aws:iam::756629837203:role/catalogue-developer"
API_KEY_SECRET_ID="elastic_cloud/concepts_pipeline_terraform/api_key"

aws configure set region eu-west-1 --profile $AWS_CLI_PROFILE
aws configure set role_arn $CATALOGUE_DEVELOPER_ARN --profile $AWS_CLI_PROFILE
aws configure set source_profile default --profile $AWS_CLI_PROFILE

EC_API_KEY=$(aws secretsmanager get-secret-value --secret-id "$API_KEY_SECRET_ID" --profile "$AWS_CLI_PROFILE" --output text --query 'SecretString')

EC_API_KEY=$EC_API_KEY terraform "$@"
