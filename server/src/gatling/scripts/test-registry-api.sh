#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetNamespaceSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetNamespaceDetailsSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionVersionSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionVersionTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetFileSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetFileTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetQuerySimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetQueryV2Simulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPISearchSimulation
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIVerifyTokenSimulation
cd src/gatling/scripts