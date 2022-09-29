PIPELINE_NAMESPACE=$1
BUILD_REF=$2
HERE=$(dirname $0)

echo "deploying $BUILD_REF to $PIPELINE_NAMESPACE"

sh $HERE/../../scripts/notify_services.sh $PIPELINE_NAMESPACE
