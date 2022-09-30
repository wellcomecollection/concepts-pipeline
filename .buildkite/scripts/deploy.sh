PIPELINE_NAMESPACE=$1
BUILD_REF=$2
ROOT=$(git rev-parse --show-toplevel)

echo "deploying $BUILD_REF to $PIPELINE_NAMESPACE"

sh "$ROOT/scripts/notify_services.sh" "$PIPELINE_NAMESPACE"
