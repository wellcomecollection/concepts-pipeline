#!/usr/bin/env bash
set -euo pipefail

SCALAFMT_VERSION=3.5.8
ROOT=$(git rev-parse --show-toplevel)

echo $PATH
# Install the formatter if it's not already present on the agent
if [[ ! -x "$(command -v scalafmt-native)" || "$(scalafmt-native --version)" != "$SCALAFMT_VERSION" ]]; then
  curl https://raw.githubusercontent.com/scalameta/scalafmt/master/bin/install-scalafmt-native.sh | \
    bash -s -- $SCALAFMT_VERSION /usr/local/bin/scalafmt-native
fi

# Run the formatter
scalafmt-native --non-interactive $ROOT

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
