package org.eclipse.openvsx

import io.gatling.core.Predef._

import org.eclipse.openvsx.Scenarios._

class RegistryAPIGetQuerySimulation extends Simulation {
  setUp(getQueryScenario().inject(atOnceUsers(users))).protocols(httpProtocol)
}
