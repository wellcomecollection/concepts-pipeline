#!/usr/bin/env bash
set -e
AWS_CLI_PROFILE="concepts-aggregator-local"
CATALOGUE_DEVELOPER_ARN="arn:aws:iam::756629837203:role/catalogue-developer"
ROOT=$(git rev-parse --show-toplevel)

CONCEPTS_PIPELINE_DATE=$1
# If the "date" is less than 10 chars, then it is probably a Work Id
# and someone forgot to add the date
if [ "$CONCEPTS_PIPELINE_DATE" = "" ] ||  [ ${#CONCEPTS_PIPELINE_DATE} -lt 10 ] ; then
    echo "You must provide a pipeline date!"
    exit 1
fi
shift

AWS_PROFILE=catalogue-developer
ES_HOST="elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/public_host"
ES_PASSWORD="elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/aggregator/password"
APP_CONTEXT=remote
(cd $ROOT && java -jar target/aggregator.jar "$@")
