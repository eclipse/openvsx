package org.eclipse.openvsx

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class RegistryAPICreateNamespaceSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .disableCaching

  val users = 5
  val namespacesCount = 780
  val repeatTimes = namespacesCount / users

  val scn = scenario("RegistryAPI: Create Namespace")
    .repeat(repeatTimes) {
      feed(csv("namespaces.csv"))
        .feed(csv("access-tokens.csv").circular)
        .exec(http("RegistryAPI.createNamespace")
        .post(s"/api/-/namespace/create")
        .queryParam("token", """${access_token}""")
        .body(StringBody("""{"name":"${namespace}"}""")).asJson
        .requestTimeout(3.minutes)
        .check(status.is(201)))
//      useful for debugging responses
//        .check(bodyString.saveAs("BODY")))
//        .exec(session => {
//          val response = session("BODY").as[String]
//          println(s"Response body: \n$response")
//          session
//        })
    }

  setUp(scn.inject(atOnceUsers(users))).protocols(httpProtocol)
}
