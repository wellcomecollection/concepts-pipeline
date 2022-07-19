#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o verbose

PROJECT="$@"
ROOT=$(git rev-parse --show-toplevel)
SBT=$ROOT/.buildkite/scripts/sbt.sh
CATALOGUE_ECR_REPO="756629837203.dkr.ecr.eu-west-1.amazonaws.com"

$SBT "project $PROJECT" "test"

if [ "$BUILDKITE_BRANCH" == "ingestor-ecr" ]; then
  PROJECT_DIR=$($SBT -error "project $PROJECT" "stage; baseDirectory")
  IMAGE_TAG="$PROJECT:ref.$BUILDKITE_COMMIT"

  docker build "$ROOT/$PROJECT_DIR" --tag $IMAGE_TAG
  docker tag $IMAGE_TAG "$CATALOGUE_ECR_REPO/$IMAGE_TAG"
  docker tag $IMAGE_TAG "$CATALOGUE_ECR_REPO/$PROJECT:latest"
  docker push --all-tags "$CATALOGUE_ECR_REPO/$PROJECT"
fi
