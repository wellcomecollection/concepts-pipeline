#!/usr/bin/env bash
set -e

AWS_CLI_PROFILE="concepts-aggregator-local"
CATALOGUE_DEVELOPER_ARN="arn:aws:iam::756629837203:role/catalogue-developer"

CONCEPTS_PIPELINE_DATE=$1
if [ "$CONCEPTS_PIPELINE_DATE" = "" ]; then
    echo "You must provide a pipeline date!"
    exit 1
fi
shift

aws configure set region eu-west-1 --profile $AWS_CLI_PROFILE
aws configure set role_arn $CATALOGUE_DEVELOPER_ARN --profile $AWS_CLI_PROFILE
aws configure set source_profile default --profile $AWS_CLI_PROFILE

function get_es_secret () {
  aws secretsmanager get-secret-value \
    --secret-id "$1" \
    --profile "$AWS_CLI_PROFILE" \
    --output text \
    --query 'SecretString'
}

ES_HOST=$(get_es_secret "elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/public_host")
ES_PORT=9243
ES_SCHEME="https"
ES_USERNAME="aggregator"
ES_PASSWORD=$(get_es_secret "elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/aggregator/password")

docker compose run \
  --env es_host="$ES_HOST" \
  --env es_port="$ES_PORT" \
  --env es_scheme="$ES_SCHEME" \
  --env es_username="$ES_USERNAME" \
  --env es_password="$ES_PASSWORD" \
  aggregator "$@"
