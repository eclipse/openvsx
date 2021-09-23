package org.eclipse.openvsx

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RegistryAPIGetNamespaceSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("http://localhost:8080")
		.disableCaching

	val scn = scenario("RegistryAPI: Get Namespace")
		.repeat(1000) {
			feed(csv("namespaces.csv").circular)
				.exec(http("RegistryAPI.getNamespace")
					.get("""/api/${namespace}""")
					.check(status.is(200)))
		}

	setUp(scn.inject(atOnceUsers(5))).protocols(httpProtocol)
}