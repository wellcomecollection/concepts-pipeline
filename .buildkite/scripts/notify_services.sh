# Tell the lambda to update its image
PIPELINE_NAMESPACE=$1

# It may seem counterintuitive,
# that this call is always the same on a pipeline
# updating with the same image uri as is already in use.
# This is because Lambda will not automagically fetch the tagged image when it next runs,
# It needs to be told that a new one is available.

# https://docs.aws.amazon.com/lambda/latest/dg/python-image.html#python-image-deploy
aws lambda update-function-code --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator --image-uri 756629837203.dkr.ecr.eu-west-1.amazonaws.com/weco/concepts_aggregator:${PIPELINE_NAMESPACE}

# Waiting here and then reading the configuration is not a necessary step,
# but it may aid investigations if something goes awry.
aws lambda wait function-updated --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator
aws lambda get-function-configuration --function-name ${PIPELINE_NAMESPACE}-concepts_aggregator

