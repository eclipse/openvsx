#!/usr/bin/env bash

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )

cd "${SERVER_ROOT}"
jbang scripts/src/ImportSort.java src/main/java src/test/java
jbang jbang-fmt@jbangdev @config/jbang-fmt.args src/main/java src/test/java
jbang scripts/src/ClosingBraceFix.java src/main/java src/test/java
