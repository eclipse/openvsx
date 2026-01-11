#!/bin/sh

cd ../../..
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.adapter.VSCodeAdapterExtensionQuerySimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.adapter.VSCodeAdapterGetAssetSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.adapter.VSCodeAdapterItemSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.adapter.VSCodeAdapterUnpkgSimulation
./gradlew --rerun-tasks gatlingRun --simulation=org.eclipse.openvsx.adapter.VSCodeAdapterVspackageSimulation
cd src/gatling/scripts || exit
