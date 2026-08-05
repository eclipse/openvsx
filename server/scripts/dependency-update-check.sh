#!/usr/bin/env bash

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )

cd "${SERVER_ROOT}"

./gradlew listDependencies --quiet

jbang toolbox@maveniverse versions --force-updates build/dependencies/list.txt --artifactVersionSelectorSpec="minor()" | grep -v "up to date" > gradle/dependency-updates.txt

if git diff --exit-code --quiet gradle/dependency-updates.txt; then
    echo "All dependencies are up to date."
else
    git diff gradle/dependency-updates.txt
    exit 1
fi
