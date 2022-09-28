## Quick Start

### Using Gitpod

To get started quickly, it is recommended to use Gitpod as default with the development environment already configured.

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/eclipse/openvsx)

### Setup locally on WSL

- Install WSL

  - [https://learn.microsoft.com/en-us/windows/wsl/setup/environment?source=recommendations](https://learn.microsoft.com/en-us/windows/wsl/setup/environment?source=recommendations)

- Make sure the firewall is not blocking internet access on WSL2 - need to check every time before running the application or installing new tools.

  - Try to ping a random website, for example, “ping -c 3 [www.google.ca](http://www.google.ca)”, if fails, proceed below
  - sudo vi /etc/resolv.conf
  - Change nameserver to 8.8.8.8
  - Ping google again to see if it works this time
  - Related issue: [https://github.com/microsoft/WSL/issues/3669](https://github.com/microsoft/WSL/issues/3669)
  - Solution that I’m following: [https://stackoverflow.com/questions/60269422/windows10-wsl2-ubuntu-debian-no-network](https://stackoverflow.com/questions/60269422/windows10-wsl2-ubuntu-debian-no-network)

- clone the repository from github
- set up vscode on WSL

  - [https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-vscode](https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-vscode)

- Install postgres

  - sudo apt update
  - sudo apt install postgresql postgresql-contrib
  - sudo service postgresql start
  - sudo service postgresql status (this is to check if the service is really on)
  - sudo -u postgres psql (connect to the postgres service)
    - CREATE ROLE gitpod with LOGIN PASSWORD ‘gitpod’;
  - [https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-database](https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-database)

- Install node.js using nvm

  - [https://docs.microsoft.com/en-us/windows/dev-environment/javascript/nodejs-on-wsl](https://docs.microsoft.com/en-us/windows/dev-environment/javascript/nodejs-on-wsl)

- Set up JDK 11

  - sudo apt install onpenjdk-11-jdk

- Install docker

  - [https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-containers](https://learn.microsoft.com/en-us/windows/wsl/tutorials/wsl-containers)
  - [https://www.youtube.com/watch?v=idW-an99TAM](https://www.youtube.com/watch?v=idW-an99TAM)

- Instal Elasticsearch with Docker

  - sudo docker pull elasticsearch:7.9.3
  - sudo docker run -d -name elasticsearch -p 9200:9200 -p 9300:9300 -e “discovery.type=single-node” elasticsearch:7.9.3

- cd cli

  - yarn install
  - yarn build

- cd server

  - ./scripts/generate-properties.sh
  - ./gradlew build
  - ./gradlew runServer

- cd webui
  - yarn install
  - yarn build
  - yarn build:default
  - yarn start:default
- Go to localhost:3000 on browser and it should be up and running
