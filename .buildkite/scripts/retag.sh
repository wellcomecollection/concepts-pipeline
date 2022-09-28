AGGREGATOR_IMAGE_TAG=$1
PIPELINE_NAMESPACE=$2

MANIFEST=$(aws ecr batch-get-image --repository-name weco/concepts_aggregator --image-ids imageTag=AGGREGATOR_IMAGE_TAG --output json | jq --raw-output --join-output '.images[0].imageManifest')
aws ecr put-image --repository-name weco/concepts_aggregator  --image-tag ${PIPELINE_NAMESPACE} --image-manifest "$MANIFEST"
