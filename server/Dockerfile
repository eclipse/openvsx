FROM openjdk:11.0.7-jdk

# Create user openvsx and set up home directory
RUN groupadd -r openvsx && useradd --no-log-init -r -g openvsx openvsx
RUN mkdir -p /home/openvsx/server && chown -R openvsx:openvsx /home/openvsx
USER openvsx
WORKDIR /home/openvsx/server

# Copy and unpack the server archive
ADD --chown=openvsx:openvsx build/libs/openvsx-server.jar /home/openvsx/
RUN jar -xf /home/openvsx/openvsx-server.jar && rm /home/openvsx/openvsx-server.jar

# Copy the start script and make it executable
ADD --chown=openvsx:openvsx scripts/run-server.sh ./
RUN chmod u+x run-server.sh

# Run sthe start script
ENTRYPOINT ["./run-server.sh"]
