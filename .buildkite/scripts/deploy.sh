PIPELINE_NAMESPACE=$1
SERVICE=$2
BUILD_REF=$3
ROOT=$(git rev-parse --show-toplevel)

echo "deploying $BUILD_REF to $PIPELINE_NAMESPACE"

sh "$ROOT/scripts/notify_lambda.sh" "$PIPELINE_NAMESPACE" $SERVICE