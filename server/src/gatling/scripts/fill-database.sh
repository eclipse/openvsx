#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPICreateNamespaceSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIPublishExtensionSimulation
cd src/gatling/scripts