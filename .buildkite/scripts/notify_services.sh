# Tell the lambda to update its image
# Lambda will not automagically fetch the tagged image when it next runs,
# It needs to be told that a new one is available.
PIPELINE_NAMESPACE=$1

aws lambda update-function-code  --region eu-west-1 --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator --image-uri 756629837203.dkr.ecr.eu-west-1.amazonaws.com/weco/concepts_aggregator:${PIPELINE_NAMESPACE}

aws lambda wait function-updated  --region eu-west-1 --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator

aws lambda get-function  --region eu-west-1 --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator

