#!/usr/bin/env bash
set -e

CONCEPTS_PIPELINE_DATE=$1
if [ "$CONCEPTS_PIPELINE_DATE" = "" ]; then
    echo "You must provide a pipeline date!"
    exit 1
fi
shift

ROOT=$(git rev-parse --show-toplevel)
cd $ROOT

export AWS_PROFILE=catalogue-developer
export APP_CONTEXT=remote
export secrets_resolver=AWSDefault
export es_host="elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/public_host"
export es_password="elasticsearch/concepts-$CONCEPTS_PIPELINE_DATE/ingestor/password"

sbt "project ingestor" run "$@"



