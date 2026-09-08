#!/usr/bin/env bash

# Checks that every place declaring org.eclipse.jdt:org.eclipse.jdt.core agrees with the version
# Spotless provisions for the eclipse('<N>') formatter step in build.gradle. See
# scripts/src/JdtVersionCheck.java for the actual check - it discovers the declaration sites from
# the files themselves, so there is nothing to keep in sync here.
#
# Usage: jdt-version-check.sh

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )

cd "${SERVER_ROOT}"

jbang scripts/src/JdtVersionCheck.java "$@"
