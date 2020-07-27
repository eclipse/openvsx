FROM gradle:jdk11 as builder

# Copy sources
WORKDIR /home/gradle
COPY --chown=gradle:gradle gradlew *.gradle ./
COPY --chown=gradle:gradle gradle ./gradle/
COPY --chown=gradle:gradle src ./src/

# Build the server application
RUN ./gradlew --no-daemon assemble


FROM openjdk:11.0.7-jdk

# Create user openvsx and set up home directory
RUN groupadd -r openvsx && useradd --no-log-init -r -g openvsx openvsx
RUN mkdir -p /home/openvsx/server && chown -R openvsx:openvsx /home/openvsx
USER openvsx
WORKDIR /home/openvsx/server

# Copy and unpack the server archive
COPY --chown=openvsx:openvsx --from=builder /home/gradle/build/libs/openvsx-server.jar /home/openvsx/
RUN jar -xf /home/openvsx/openvsx-server.jar && rm /home/openvsx/openvsx-server.jar

# Copy the start script and make it executable
COPY --chown=openvsx:openvsx scripts/run-server.sh ./
RUN chmod u+x run-server.sh

# Run sthe start script
ENTRYPOINT ["./run-server.sh"]
