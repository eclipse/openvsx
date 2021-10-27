# Setup
## build.gradle
### Running on dev environment:
- comment out `devRuntimeOnly "org.springframework.boot:spring-boot-devtools"` to prevent spring boot restart when you compile a gatling simulation.
- add `jvmArgs = ['-Xverify:none']` to runServer task if you want to attach VisualVM to the server.

### Running against remote server:
- add `jvmArgs = ['-Xverify:none', '-Dcom.sun.management.jmxremote', '-Dcom.sun.management.jmxremote.port=<PORT>', '-Dcom.sun.management.jmxremote.authenticate=false', '-Dcom.sun.management.jmxremote.ssl=false', '-Djava.rmi.server.hostname=<IP_ADDRESS>']` to runServer task if you want to attach VisualVM to the server. Change **<PORT>** and **<IP_ADDRESS>** to the server IP address and the port JMX must listen on.

## Gatling simulations
### Running against remote server:
- Change `baseUrl` property in `resources/application.properties`.
- If the remote server requires authentication, uncomment and change the `auth` property in `resources/application.properties`. 
  Simulations use the `auth` value to set the Authorization request header.

### resources/access-tokens.csv:
- contains API access tokens. Create a couple of tokens using the web UI and add them to this file. 
- make sure to keep the `access_token` header at the top of the file.

### scala/org/eclipse/openvsx/RegistryAPIPublishExtensionSimulation.scala:
- Change `extensionDir` property in `resources/application.properties` to a directory that **only** contains extensions (*.vsix files). The simulation uses those files to upload them to the server.

# Running Gatling
**The Gatling 'post' simulations need to be run to fill the database:**
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPICreateNamespaceSimulation`
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIPublishExtensionSimulation`

**After running those simulations all other simulations can be run in no particular order:**
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetNamespaceSimulation`
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionSimulation`
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetExtensionVersionSimulation`
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.RegistryAPIGetQuerySimulation`
- `./gradlew --rerun-tasks gatlingRun-org.eclipse.openvsx.adapter.VSCodeAdapterExtensionQuerySimulation`

## Empty the database
If you wish to empty the database after running the Gatling simulations, you can run:
```BEGIN;
DELETE FROM file_resource;
-- extension.latest and extension.preview refer back to extension_version.id
ALTER TABLE extension_version DISABLE TRIGGER ALL;
DELETE FROM extension_version;
ALTER TABLE extension_version ENABLE TRIGGER ALL;
DELETE FROM extension;
DELETE FROM namespace_membership;
DELETE FROM namespace;
COMMIT;