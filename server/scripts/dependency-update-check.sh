#!/usr/bin/env bash

# Reports which runtime/test dependencies have a newer minor-version release available.
# Informational only - it never fails, so there is nothing to review-and-recommit to keep this
# green; run it whenever you want a fresh picture of the update backlog.
#
# For a hard gate on the curated Spring Boot BOM overrides in build.gradle, see
# dependency-override-check.sh instead.

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )

cd "${SERVER_ROOT}"

./gradlew listDependencies --quiet

updates=$(jbang toolbox@maveniverse versions --force-updates build/dependencies/list.txt --artifactVersionSelectorSpec="minor()" | grep -v "up to date" || true)

if [ -z "${updates}" ]; then
    echo "All dependencies are up to date."
else
    echo "${updates}"
fi
