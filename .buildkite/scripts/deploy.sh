PIPELINE_NAMESPACE=$1
BUILD_REF=$2
ROOT=$(git rev-parse --show-toplevel)

echo "deploying $BUILD_REF to $PIPELINE_NAMESPACE"

sh "$ROOT/scripts/notify_lambda.sh" "$PIPELINE_NAMESPACE" "concepts_aggregator"
sh "$ROOT/scripts/notify_lambda.sh" "$PIPELINE_NAMESPACE" "concepts_ingestor"
