#!/usr/bin/env bash
set -euo pipefail

SCALAFMT_VERSION=3.5.8
TERRAFORM_VERSION=1.2.5
ROOT=$(git rev-parse --show-toplevel)
PATH=$HOME/.local/bin:$PATH

# Install the formatters if they're not already present on the agent
if [[ ! -x "$(command -v scalafmt-native)" || "$(scalafmt-native --version)" != "$SCALAFMT_VERSION" ]]; then
  mkdir -p $HOME/.local/bin
  curl https://raw.githubusercontent.com/scalameta/scalafmt/master/bin/install-scalafmt-native.sh | \
    bash -s -- $SCALAFMT_VERSION $HOME/.local/bin/scalafmt-native
fi
if [[ ! -x "$(command -v terraform)" || "$(terraform --version)" != "$TERRAFORM_VERSION" ]]; then
  mkdir -p $HOME/.local/bin
  wget -O /tmp/terraform.zip https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
  unzip -q -d $HOME/.local/bin /tmp/terraform.zip
fi

# Run the formatters
scalafmt-native --non-interactive $ROOT
terraform fmt -recursive $ROOT

# Commit any changes
if [[ `git status --porcelain` ]]; then
  git config user.name "Buildkite on behalf of Wellcome Collection"
  git config user.email "wellcomedigitalplatform@wellcome.ac.uk"

  git remote add ssh-origin $BUILDKITE_REPO || true
  git fetch ssh-origin
  git checkout --track ssh-origin/$BUILDKITE_BRANCH || true

  git add --verbose --update
  git commit -m "Apply auto-formatting rules"

  git push ssh-origin HEAD:$BUILDKITE_BRANCH
  exit 1;
else
  echo "There were no changes from auto-formatting"
  exit 0;
fi
