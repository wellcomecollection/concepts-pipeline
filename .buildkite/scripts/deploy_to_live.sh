AGGREGATOR_IMAGE_TAG=$1
PIPELINE_NAMESPACE=`cat infrastructure/live_pipeline.txt`
HERE=$(dirname $0)

echo $PIPELINE_NAMESPACE
echo $AGGREGATOR_IMAGE_TAG
echo "current lambda configuration, before change"
aws lambda get-function-configuration --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator

echo "updating lambda configuration"
sh $HERE/retag.sh weco/concepts_aggregator ref.${AGGREGATOR_IMAGE_TAG} $PIPELINE_NAMESPACE
sh $HERE/notify_services.sh $PIPELINE_NAMESPACE
