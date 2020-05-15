FROM openjdk:11.0.7-jdk

# Create user openvsx
RUN groupadd -r openvsx && useradd --no-log-init -r -g openvsx openvsx
RUN mkdir -p /home/openvsx/server && chown -R openvsx:openvsx /home/openvsx
USER openvsx
WORKDIR /home/openvsx/server

# Unpack the server archive
ADD --chown=openvsx:openvsx build/libs/openvsx-server.jar /home/openvsx/
RUN jar -xf /home/openvsx/openvsx-server.jar && rm /home/openvsx/openvsx-server.jar

# Configure executable (override CMD arguments to add JVM options)
WORKDIR /home/openvsx/server/BOOT-INF/classes:BOOT-INF/lib
ENTRYPOINT ["java"]
CMD ["org.eclipse.openvsx.RegistryApplication"]
