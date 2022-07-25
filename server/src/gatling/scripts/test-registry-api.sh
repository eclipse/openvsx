#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetNamespaceSimulation
read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionSimulation
read -s -n 1 -p "Press any key to continue..."
# ./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionTargetPlatformSimulation
# read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionVersionSimulation
read -s -n 1 -p "Press any key to continue..."
# ./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionVersionTargetPlatformSimulation
# read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetFileSimulation
read -s -n 1 -p "Press any key to continue..."
# ./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetFileTargetPlatformSimulation
#read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetQuerySimulation
read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPISearchSimulation
cd src/gatling/scripts