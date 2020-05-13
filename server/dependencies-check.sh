#!/bin/sh

# Clone and build license tool
if [ ! -d "/workspace/dash-licenses" ] 
then
    cd /workspace
    git clone https://github.com/eclipse/dash-licenses.git
    cd dash-licenses
    mvn package
fi

# Generate build/dependencies/list.txt
cd /workspace/openvsx/server
./gradlew listDependencies

# Generate DEPENDENCIES report
java -jar /workspace/dash-licenses/target/org.eclipse.dash.licenses-0.0.1-SNAPSHOT.jar build/dependencies/list.txt
