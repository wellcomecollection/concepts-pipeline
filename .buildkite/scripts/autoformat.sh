#!/usr/bin/env bash

SCALAFMT_VERSION=3.5.8
ROOT=$(git rev-parse --show-toplevel)

if [[ ! -x "$(command -v scalafmt-native)" || "$(scalafmt-native --version)" != "$SCALAFMT_VERSION" ]]; then
  curl https://raw.githubusercontent.com/scalameta/scalafmt/master/bin/install-scalafmt-native.sh | \
    bash -s -- $SCALAFMT_VERSION /usr/local/bin/scalafmt-native
fi

scalafmt-native --non-interactive $ROOT

if [[ `git status --porcelain` ]]; then
  git config user.name "Buildkite on behalf of Wellcome Collection"
  git config user.email "wellcomedigitalplatform@wellcome.ac.uk"

  git remote add ssh-origin $BUILDKITE_REPO
  git fetch ssh-origin
  git checkout --track ssh-origin/$BUILDKITE_BRANCH

  git add --verbose --update
  git commit -m "Apply auto-formatting rules"

  git push ssh-origin HEAD:$BUILDKITE_BRANCH
  exit 1;
else
  echo "There were no changes from auto-formatting"
  exit 0;
fi