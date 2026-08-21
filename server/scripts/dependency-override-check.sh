#!/usr/bin/env bash

# Checks the Spring Boot managed-version overrides declared in build.gradle against reality.
# See scripts/src/DependencyOverrideCheck.java for the actual check - it discovers the overrides
# from build.gradle's `ext[...]` block itself, so there is nothing to keep in sync here.
#
# Usage: dependency-override-check.sh [--latest]

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )

cd "${SERVER_ROOT}"

jbang scripts/src/DependencyOverrideCheck.java "$@"
