/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
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

class RegistryAPIGetFileTargetPlatformSimulation extends Simulation {
  setUp(getFileTargetPlatformScenario().inject(atOnceUsers(users))).protocols(httpProtocol)
}
