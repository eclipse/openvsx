#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetNamespaceSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetNamespaceDetailsSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetExtensionSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetExtensionTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetExtensionVersionSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetExtensionVersionTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetVersionReferencesSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetVersionReferencesTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetFileSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetFileTargetPlatformSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetQuerySimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIGetQueryV2Simulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPISearchSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.RegistryAPIVerifyTokenSimulation
cd src/gatling/scripts || exit
