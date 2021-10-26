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

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RegistryAPIGetNamespaceSimulation extends Simulation {
	val conf = ConfigFactory.load()

	val httpProtocol = http
		.baseUrl(conf.getString("baseUrl"))
		.disableCaching

	var headers: Map[String,String] = Map()
	if(conf.hasPath("auth")) {
		headers += "Authorization" -> conf.getString("auth");
	}

	val scn = scenario("RegistryAPI: Get Namespace")
		.repeat(1000) {
			feed(csv("namespaces.csv").circular)
				.exec(http("RegistryAPI.getNamespace")
					.get("""/api/${namespace}""")
					.headers(headers)
					.check(status.is(200)))
		}

	setUp(scn.inject(atOnceUsers(5))).protocols(httpProtocol)
}