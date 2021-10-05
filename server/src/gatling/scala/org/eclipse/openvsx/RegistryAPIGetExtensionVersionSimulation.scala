package org.eclipse.openvsx

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RegistryAPIGetExtensionVersionSimulation extends Simulation {
  val conf = ConfigFactory.load()

  val httpProtocol = http
    .baseUrl(conf.getString("baseUrl"))
    .disableCaching

  var headers: Map[String,String] = Map()
  if(conf.hasPath("auth")) {
    headers += "Authorization" -> conf.getString("auth");
  }

  val scn = scenario("RegistryAPI: Get Extension Version")
    .repeat(744) {
      feed(csv("extension-versions.csv"))
        .exec(http("RegistryAPI.getExtensionVersion")
          .get("""/api/${namespace}/${name}/${version}""")
          .headers(headers))
//          .check(status.is(200)))
    }

  setUp(scn.inject(atOnceUsers(4))).protocols(httpProtocol)
}
