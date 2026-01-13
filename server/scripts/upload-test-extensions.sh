#!/usr/bin/env bash
set -u

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SERVER_ROOT=$( dirname "${SCRIPT_DIR}" )
PROJECT_ROOT=$( dirname "${SERVER_ROOT}" )

TEST_EXTENSIONS_DIRECTORY="${SERVER_ROOT}/build/test-extensions"

if [ ! -d "${TEST_EXTENSIONS_DIRECTORY}" ]; then
  echo "'${TEST_EXTENSIONS_DIRECTORY}' does not exist."
  echo "Downloading test extensions..."
  cd "${SERVER_ROOT}" && ./gradlew downloadTestExtensions
fi

export OVSX_REGISTRY_URL="${1:-http://localhost:8080}"
export OVSX_PAT=super_token
export PUBLISHERS="DotJoshJohnson eamodio felixfbecker formulahendry HookyQR ms-azuretools ms-mssql ms-python ms-vscode octref redhat ritwickdey sburg vscode vscodevim Wscats"

echo "Publishing test extensions to '${OVSX_REGISTRY_URL}'"

echo "Creating namespaces..."
for pub in $PUBLISHERS; do ${PROJECT_ROOT}/cli/bin/ovsx create-namespace "${pub}"; done

echo "Publishing extensions..."
find "${SERVER_ROOT}/build/test-extensions-builtin" -name '*.vsix' -exec ${PROJECT_ROOT}/cli/bin/ovsx publish '{}' \;
find "${SERVER_ROOT}/build/test-extensions" -name '*.vsix' -exec ${PROJECT_ROOT}/cli/bin/ovsx publish '{}' \;
