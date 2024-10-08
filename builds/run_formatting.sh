#!/usr/bin/env bash
set -euo pipefail

SCALAFMT_VERSION=3.5.9
TERRAFORM_VERSION=1.2.5
ROOT=$(git rev-parse --show-toplevel)
PATH=$HOME/.local/bin:$PATH

# Install the formatters if they're not already present on the agent
if [[ ! -x "$(command -v scalafmt-native)" || "$(scalafmt-native --version)" != "$SCALAFMT_VERSION" ]]; then
  mkdir -p $HOME/.local/bin
  curl https://raw.githubusercontent.com/scalameta/scalafmt/master/bin/install-scalafmt-native.sh | \
    bash -s -- $SCALAFMT_VERSION $HOME/.local/bin/scalafmt-native
fi
if [[ ! -x "$(command -v terraform)" || "$(terraform version -json | jq -r .terraform_version)" != "$TERRAFORM_VERSION" ]]; then
  mkdir -p $HOME/.local/bin
  wget -O /tmp/terraform.zip https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
  unzip -q -o /tmp/terraform.zip -d $HOME/.local/bin
fi

# Run the formatters
scalafmt-native --non-interactive $ROOT
terraform fmt -recursive $ROOT
