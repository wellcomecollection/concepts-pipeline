#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o verbose

SBT_VERSION="1.7.1"
ECR_REGISTRY="760097843905.dkr.ecr.eu-west-1.amazonaws.com"
ROOT=$(git rev-parse --show-toplevel)

docker run --tty --rm \
  --volume ~/.sbt:/root/.sbt \
  --volume ~/.cache/coursier/v1/:/root/.cache/coursier/v1 \
  --volume $ROOT:/repo \
  "$ECR_REGISTRY/wellcome/sbt_wrapper:$SBT_VERSION" "$@"
