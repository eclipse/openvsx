#!/bin/sh

# This script is included in the Docker image to run the server application
echo "JVM_ARGS: '${JVM_ARGS}'"

# shellcheck disable=SC2086
exec java ${JVM_ARGS} -cp BOOT-INF/classes:BOOT-INF/lib/* org.eclipse.openvsx.RegistryApplication
