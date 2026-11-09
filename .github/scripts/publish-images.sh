#!/usr/bin/env bash
# Builds and pushes the backend and frontend images to GHCR for one release.
# Called by semantic-release (release.config.js) with the new version, e.g. 0.3.0.
# Expects to run in GitHub Actions after docker login and buildx setup.
set -euo pipefail

VERSION="$1"
MAJOR_MINOR="${VERSION%.*}"
OWNER="${GITHUB_REPOSITORY_OWNER,,}" # image names must be lowercase
SOURCE="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}"

# The version is written in the pom of this run only: it is never committed
(cd backend && ./mvnw -B -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false)

for app in backend frontend; do
  image="ghcr.io/${OWNER}/helper-730-${app}"
  echo "Publishing ${image}:${VERSION}"
  docker buildx build "$app" --push \
    --tag "${image}:${VERSION}" \
    --tag "${image}:${MAJOR_MINOR}" \
    --tag "${image}:latest" \
    --label "org.opencontainers.image.source=${SOURCE}" \
    --label "org.opencontainers.image.version=${VERSION}" \
    --label "org.opencontainers.image.revision=${GITHUB_SHA}"
done
