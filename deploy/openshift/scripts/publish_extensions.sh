#!/bin/bash
# This script automates the process of downloading, configuring, and publishing VS Code extensions to an OpenVSX registry 
# deployed on an OpenShift cluster.

# Path to the extensions.txt file
current_dir=$(pwd)
EXTENSIONS_FILE="$current_dir/extensions.txt"

listOfPublishers=()
containsElement () { for e in "${@:2}"; do [[ "$e" = "$1" ]] && return 0; done; return 1; }

# Get pod name where ovsx is installed
OVSX_POD_NAME=$(kubectl get pods -n "$OPENVSX_NAMESPACE" -o jsonpath="{.items[*].metadata.name}" \
  | tr ' ' '\n' | grep '^ovsx-cli' || true)

if [ -z "$OVSX_POD_NAME" ]; then
  OVSX_POD_NAME=$(kubectl get pods -n "$OPENVSX_NAMESPACE" -o jsonpath="{.items[*].metadata.name}" \
    | tr ' ' '\n' | grep '^openvsx-server' || true)
fi

export OVSX_POD_NAME

# Read the extensions.txt file line by line
while IFS= read -r line; do
   # Extract the vsix file name from the URL
   vsix_url="$line"
   vsix_filename=$(basename "$vsix_url")
  
   # Download the vsix file into the /tmp directory
   echo "Downloading $vsix_url"
   kubectl exec -n "${OPENVSX_NAMESPACE}" "${OVSX_POD_NAME}" -- bash -c "wget -q -P /tmp '$vsix_url' "

   # Extract namespace_name (everything before the first .)
   namespace_name=$(echo "$vsix_filename" | cut -d. -f1)

   # Execute ovsx commands
   # check if publisher is in the list of publishers
   if ! containsElement "${namespace_name}" "${listOfPublishers[@]}"; then
       listOfPublishers+=("${namespace_name}")
       # create namespace
       kubectl exec -n "${OPENVSX_NAMESPACE}" "${OVSX_POD_NAME}" -- bash -c "ovsx create-namespace '$namespace_name'" || true
   fi
   # publish extension
   kubectl exec -n "${OPENVSX_NAMESPACE}" "${OVSX_POD_NAME}" -- bash -c "ovsx publish /tmp/'$vsix_filename'"

   # remove the downloaded file
   kubectl exec -n "${OPENVSX_NAMESPACE}" "${OVSX_POD_NAME}" -- bash -c "rm /tmp/'$vsix_filename'"
done < "$EXTENSIONS_FILE"
