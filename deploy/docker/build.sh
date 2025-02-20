#/bin/bash

export OPENVSX_VERSION=`curl -sSL https://api.github.com/repos/eclipse/openvsx/releases/latest | jq -r ".tag_name"`
sudo docker build -t "openvsx:$OPENVSX_VERSION" --build-arg "OPENVSX_VERSION=$OPENVSX_VERSION" .