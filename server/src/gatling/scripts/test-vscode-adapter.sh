#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterExtensionQuerySimulation
read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterGetAssetSimulation
read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterItemSimulation
read -s -n 1 -p "Press any key to continue..."
# ./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterUnpkgSimulation
# read -s -n 1 -p "Press any key to continue..."
./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterVspackageSimulation
cd src/gatling/scripts