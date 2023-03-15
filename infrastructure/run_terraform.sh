#!/usr/bin/env bash
ROOT=$(git rev-parse --show-toplevel)
if ! sh "$ROOT/infrastructure/is_up_to_date.sh";
then
  exit 1
fi

AWS_CLI_PROFILE="concepts-pipeline-terraform"
CATALOGUE_DEVELOPER_ARN="arn:aws:iam::756629837203:role/catalogue-developer"
API_KEY_SECRET_ID="elastic_cloud/concepts_pipeline_terraform/api_key"

aws configure set region eu-west-1 --profile $AWS_CLI_PROFILE
aws configure set role_arn $CATALOGUE_DEVELOPER_ARN --profile $AWS_CLI_PROFILE
aws configure set source_profile default --profile $AWS_CLI_PROFILE

EC_API_KEY=$(aws secretsmanager get-secret-value --secret-id "$API_KEY_SECRET_ID" --profile "$AWS_CLI_PROFILE" --output text --query 'SecretString')

EC_API_KEY=$EC_API_KEY terraform "$@"
