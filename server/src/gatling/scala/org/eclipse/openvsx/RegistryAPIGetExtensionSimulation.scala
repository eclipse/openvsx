package org.eclipse.openvsx

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RegistryAPIGetExtensionSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .disableCaching

  val scn = scenario("RegistryAPI: Get Extension")
    .repeat(342) {
      feed(csv("extensions.csv"))
        .exec(http("RegistryAPI.getExtension")
          .get("""/api/${namespace}/${name}"""))
//          .check(status.is(200)))
    }

  setUp(scn.inject(atOnceUsers(4))).protocols(httpProtocol)
}
