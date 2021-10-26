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

import scala.concurrent.duration.DurationInt

class RegistryAPICreateNamespaceSimulation extends Simulation {
  val conf = ConfigFactory.load()

  val httpProtocol = http
    .baseUrl(conf.getString("baseUrl"))
    .disableCaching

  var headers: Map[String,String] = Map()
  if(conf.hasPath("auth")) {
    headers += "Authorization" -> conf.getString("auth");
  }

  val users = 5
  val namespacesCount = 780
  val repeatTimes = namespacesCount / users
  val scn = scenario("RegistryAPI: Create Namespace")
    .repeat(repeatTimes) {
      feed(csv("namespaces.csv"))
        .feed(csv("access-tokens.csv").circular)
        .exec(http("RegistryAPI.createNamespace")
        .post(s"/api/-/namespace/create")
        .headers(headers)
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
