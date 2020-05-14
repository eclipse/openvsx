FROM openjdk:11.0.6-jdk

RUN groupadd -r openvsx && useradd --no-log-init -r -g openvsx openvsx
RUN mkdir -p /home/openvsx/server && chown -R openvsx:openvsx /home/openvsx
USER openvsx
WORKDIR /home/openvsx/server

ADD --chown=openvsx:openvsx build/libs/openvsx-server.jar /home/openvsx/

RUN jar -xf /home/openvsx/openvsx-server.jar

ENTRYPOINT ["java", "-Xmx7G", "-cp", "BOOT-INF/classes:BOOT-INF/lib/*", "org.eclipse.openvsx.RegistryApplication"]
