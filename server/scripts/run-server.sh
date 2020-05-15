#!/bin/sh

echo "JVM_ARGS: '$JVM_ARGS'"

exec java $JVM_ARGS -cp BOOT-INF/classes:BOOT-INF/lib/* org.eclipse.openvsx.RegistryApplication
