#!/bin/bash

set -e

# Publish to BinTray if the HEAD commit is tagged with a version number.

if [ "$PULL_REQUEST" = "true" ]; then
  echo "Shippable is building a pull request, not publishing."
  exit 0
fi

headTag=$(git describe --exact-match ||:)
echo "Current tag is: $headTag"
PULL_REQUEST="false"
headTag=v111.222.333-0

if [ $PULL_REQUEST = "false" ] && [[ $headTag =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9-]+)?$ ]]; then
  echo "Going to release from tag $headTag."
  version=$(echo $headTag | sed -e s/^v//)

  sbt publish
  echo "Successfully published artifact."

  exit 0
fi
