# Take an image that is already on ECR, and give it a new tag
REPOSITORY=$1
EXISTING_TAG=$2
NEW_TAG=$3

MANIFEST=$(aws ecr batch-get-image --repository-name $REPOSITORY --image-ids imageTag=${EXISTING_TAG} --output json | jq --raw-output --join-output '.images[0].imageManifest')

aws ecr put-image --repository-name $REPOSITORY  --image-tag ${NEW_TAG} --image-manifest "$MANIFEST"
