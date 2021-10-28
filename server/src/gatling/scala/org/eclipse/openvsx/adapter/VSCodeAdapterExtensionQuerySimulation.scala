/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt

class VSCodeAdapterExtensionQuerySimulation extends Simulation {
  val conf = ConfigFactory.load()

  val httpProtocol = http
    .baseUrl(conf.getString("baseUrl"))
    .disableCaching

  val buildRequestBody: Expression[String] = session => {
    val query = session("query").as[String]
    val body =
      s"""
        |{
        |  "filters":[
        |    {
        |      "criteria":[
        |        {"filterType":8,"value":"Microsoft.VisualStudio.Code"},
        |        {"filterType":10,"value":"${query}"},
        |        {"filterType":12,"value":"4096"}
        |      ],
        |      "pageNumber":1,
        |      "pageSize":50,
        |      "sortBy":0,
        |      "sortOrder":0
        |    }
        |  ],
        |  "assetTypes":[],
        |  "flags":950
        |}
        |""".stripMargin

    body
  }

  var headers: Map[String,String] = Map()
  if(conf.hasPath("auth")) {
    headers += "Authorization" -> conf.getString("auth");
  }

  val scn = scenario("VSCodeAdapter: Extension Query")
    .repeat(1000) {
      feed(csv("adapter/queries-xl.csv").circular)
        .exec(http("VSCodeAdapter.extensionQuery")
          .post(s"/vscode/gallery/extensionquery")
          .headers(headers)
          .body(StringBody(buildRequestBody)).asJson
          .requestTimeout(3.minutes)
          .check(status.is(200)))
    }

  setUp(scn.inject(atOnceUsers(5))).protocols(httpProtocol)
}
