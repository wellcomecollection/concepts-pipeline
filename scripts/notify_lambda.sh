# Tell the lambda to update its image
PIPELINE_NAMESPACE=$1
SERVICE_NAME=$2

# Print out what is currently there.
echo "current lambda configuration, before change"
aws lambda get-function-configuration --function-name ${PIPELINE_NAMESPACE}-${SERVICE_NAME}

# It may seem counterintuitive,
# that this call is always the same on a pipeline
# updating with the same image uri as is already in use.
# This is because Lambda will not automagically fetch the tagged image when it next runs,
# It needs to be told that a new one is available.

echo "updating lambda configuration"
# https://docs.aws.amazon.com/lambda/latest/dg/python-image.html#python-image-deploy
aws lambda update-function-code --function-name ${PIPELINE_NAMESPACE}-${SERVICE_NAME} --image-uri 756629837203.dkr.ecr.eu-west-1.amazonaws.com/weco/${SERVICE_NAME}:${PIPELINE_NAMESPACE}

# Waiting here and then reading the configuration is not a necessary step,
# but it may aid investigations if something goes awry.
aws lambda wait function-updated --function-name ${PIPELINE_NAMESPACE}-${SERVICE_NAME}
aws lambda get-function-configuration --function-name ${PIPELINE_NAMESPACE}-${SERVICE_NAME}

