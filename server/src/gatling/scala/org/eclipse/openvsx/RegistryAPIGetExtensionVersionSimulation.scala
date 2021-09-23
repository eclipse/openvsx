package org.eclipse.openvsx

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RegistryAPIGetExtensionVersionSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .disableCaching

  val scn = scenario("RegistryAPI: Get Extension Version")
    .repeat(744) {
      feed(csv("extension-versions.csv"))
        .exec(http("RegistryAPI.getExtensionVersion")
          .get("""/api/${namespace}/${name}/${version}"""))
//          .check(status.is(200)))
    }

  setUp(scn.inject(atOnceUsers(4))).protocols(httpProtocol)
}
