/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx

import io.gatling.core.Predef._

import org.eclipse.openvsx.Scenarios._

class RegistryAPIPublishExtensionSimulation extends Simulation {
//  Utility scenario to download extensions, so that they can be published by publishScenario()
//  setUp(downloadExtensionScenario().inject(atOnceUsers(users))).protocols(httpProtocol)

  val users = 1 // publish endpoint can't handle more than 1 user right now
  setUp(publishScenario(users).inject(atOnceUsers(users))).protocols(httpProtocol)
}
