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

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

import java.nio.file.Files
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.reflect.io.File

object Scenarios {
  val conf = ConfigFactory.load()
  val users = 5
  val httpProtocol = http
    .baseUrl(conf.getString("baseUrl"))
    .disableCaching

  private def headers(): Map[String,String] = {
    var headers: Map[String,String] = Map()
    if(conf.hasPath("auth")) {
      headers += "Authorization" -> conf.getString("auth")
    }

    headers
  }

  def createNamespaceScenario(): ScenarioBuilder = {
    val namespacesCount = 780
    val repeatTimes = namespacesCount / users
    scenario("RegistryAPI: Create Namespace")
      .repeat(repeatTimes) {
        feed(csv("namespaces.csv"))
          .feed(csv("access-tokens.csv").circular)
          .exec(http("RegistryAPI.createNamespace")
            .post(s"/api/-/namespace/create")
            .headers(headers())
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
  }

  private def extensionFilesFeeder(extensionDir: String): Array[Map[String,String]] = {
    val extensionFiles = new java.io.File(extensionDir).list()
    val feeder = new Array[Map[String, String]](extensionFiles.length)

    var mapIndex = 0
    var feederIndex = 0
    // make sure that versions of same extension are not right after one another
    val extensionFilesMap = extensionFiles.groupBy(f => f.substring(0, f.lastIndexOf('-')))
    while(feederIndex < extensionFiles.length) {
      for((key, value) <- extensionFilesMap) {
        if(mapIndex < value.length) {
          val file = value(mapIndex)
          feeder(feederIndex) = Map("extension_file" -> file)
          feederIndex = feederIndex + 1
        }
      }

      mapIndex = mapIndex + 1
    }

    feeder
  }

  def publishScenario(users: Int): ScenarioBuilder = {
    val extensionDir = conf.getString("extensionDir")
    val feeder = this.extensionFilesFeeder(extensionDir)

    val repeatTimes = feeder.length / users
    scenario("RegistryAPI: Publish Extension")
      .repeat(repeatTimes) {
        feed(feeder)
          .feed(csv("access-tokens.csv").circular)
          .exec(http("RegistryAPI.publish")
            .post("/api/-/publish")
            .headers(headers())
            .queryParam("token", """${access_token}""")
            .body(ByteArrayBody(session => {
              val path = extensionDir + "\\" + session("extension_file").as[String]
              File(path).toByteArray()
            }))
            .requestTimeout(3.minutes)
            .check(status.is(201)))
      }
  }

  def downloadExtensionScenario(): ScenarioBuilder = {
    val extensionDir = conf.getString("extensionDir")
    scenario("RegistryAPI: Download Extension")
      .repeat(744) {
        feed(csv("extension-versions.csv"))
          .feed(csv("access-tokens.csv").circular)
          .exec(session => {
            val namespace = session("namespace").as[String]
            val name = session("name").as[String]
            val version = session("version").as[String]
            session.set("extension_file", s"$extensionDir\\$namespace-$name-$version.vsix")
          })
          .doIf(session => {
            val extensionFile = session("extension_file").as[String]
            println(extensionFile)
            !File(extensionFile).exists
          }) {
            exec(http("getExtensionDownloadLink")
              .get("""/api/${namespace}/${name}/${version}""")
              .check(jsonPath("$.files.download").find.saveAs("download")))
              .exec(http("downloadExtension")
                .get("""${download}""")
                .check(bodyBytes.saveAs("file_bytes")))
              .pause(30, 60)
              .exec {session =>
                Files.write(new java.io.File(session("extension_file").as[String]).toPath, session("file_bytes").as[Array[Byte]])
                session
              }
          }
      }
  }

  def getExtensionScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Extension")
      .repeat(1000) {
        feed(csv("extensions.csv").circular)
          .exec(http("RegistryAPI.getExtension")
            .get("""/api/${namespace}/${name}""")
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def getExtensionTargetPlatformScenario(): ScenarioBuilder = {
    // TODO add more target platforms besides 'universal'
    scenario("RegistryAPI: Get Extension by Target Platform")
      .repeat(1000) {
        feed(csv("extensions.csv").circular)
          .exec(http("RegistryAPI.getExtension")
            .get("""/api/${namespace}/${name}/universal""")
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def getExtensionVersionScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Extension Version")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .exec(http("RegistryAPI.getExtension")
            .get("""/api/${namespace}/${name}/${version}""")
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def getExtensionVersionTargetPlatformScenario(): ScenarioBuilder = {
    // TODO add more target platforms besides 'universal'
    scenario("RegistryAPI: Get Extension Version by Target Platform")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .exec(http("RegistryAPI.getExtension")
            .get("""/api/${namespace}/${name}/universal/${version}""")
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def getNamespaceScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Namespace")
      .repeat(1000) {
        feed(csv("namespaces.csv").circular)
          .exec(http("RegistryAPI.getNamespace")
            .get("""/api/${namespace}""")
            .headers(headers())
            .check(status.is(200)))
      }
  }

  def getNamespaceDetailsScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Namespace Details")
      .repeat(1000) {
        feed(csv("namespaces.csv").circular)
          .exec(http("RegistryAPI.getNamespaceDetails")
            .get("""/api/${namespace}/details""")
            .headers(headers())
            .check(status.is(200)))
      }
  }

  def getQueryScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Query")
      .repeat(1000) {
        feed(csv("query-strings.csv").circular)
          .exec(http("RegistryAPI.getQuery")
            .get("""/api/-/query?${query}""")
            .headers(headers())
            .check(status.is(200)))
      }
  }

  def getQueryV2Scenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Query V2")
      .repeat(1000) {
        feed(csv("query-v2-strings.csv").circular)
          .exec(http("RegistryAPI.getQueryV2")
            .get("""/api/v2/-/query?${query}""")
            .headers(headers())
            .check(status.is(200)))
      }
  }

  def getFileScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Manifest File")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .feed(Array(
            Map("file" -> ""),
            Map("file" -> "package.json"),
            Map("file" -> "extension.vsixmanifest"),
            Map("file" -> "CHANGELOG.md"),
            Map("file" -> "README.md")
          ).circular)
          .exec(http("RegistryAPI.getFile")
            .get(session => {
              val namespace = session("namespace").as[String]
              val extension = session("name").as[String]
              val version = session("version").as[String]
              var file = session("file").as[String]
              if(file.isEmpty) {
                file = namespace + "." + extension + "-" + version + ".vsix"
              }

              "/api/" + namespace + "/" + extension + "/" + version + "/file/" + file
            })
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def getFileTargetPlatformScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Get Manifest File")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .feed(Array(
            Map("file" -> """${namespace}.${name}-${version}.vsix"""),
            Map("file" -> "package.json"),
            Map("file" -> "extension.vsixmanifest"),
            Map("file" -> "CHANGELOG.md"),
            Map("file" -> "README.md")
          ).circular)
          .exec(http("RegistryAPI.getFile")
            .get(session => {
              val namespace = session("namespace").as[String]
              val extension = session("name").as[String]
              val version = session("version").as[String]
              var file = session("file").as[String]
              if(file.isEmpty) {
                file = namespace + "." + extension + "-" + version + ".vsix"
              }

              "/api/" + namespace + "/" + extension + "/universal/" + version + "/file/" + file
            })
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  private def searchQueryFeeder(): Array[Map[String,String]] = {
    var feeder = new ListBuffer[Map[String, String]]()
    val queries = Array("", "redhat", "vs", "py", "ms-vscode", "redhat.project-initializer", "ms-vscode.cmake-tools")
    val targetPlatforms = Array("", "universal")     // TODO add more target platforms besides 'universal'
    val categories = Array("", "Programming Languages", "Themes", "Snippets", "Debuggers", "Linters", "Other")
    val sizes = Array("", "5", "500")
    val offsets = Array("", "1", "100", "25000")
    val sortBys = Array("", "relevance", "timestamp", "averageRating", "downloadCount")
    val sortOrders = Array("", "asc", "desc")
    val includeAllVersions = Array("", "true", "false")

    for(query <- queries) {
      for(targetPlatform <- targetPlatforms) {
        for(category <- categories) {
          for(size <- sizes) {
            for(offset <- offsets) {
              for(sortBy <- sortBys) {
                for(sortOrder <- sortOrders) {
                  for(includeAllVersion <- includeAllVersions) {
                    var queryList = new ListBuffer[String]()
                    if(query != "") {
                      queryList += "search=" + query
                    }
                    if(targetPlatform != "") {
                      queryList += "targetPlatform=" + targetPlatform
                    }
                    if(category != "") {
                      queryList += "category=" + category
                    }
                    if(size != "") {
                      queryList += "size=" + size
                    }
                    if(offset != "") {
                      queryList += "offset=" + offset
                    }
                    if(sortBy != "") {
                      queryList += "sortBy=" + sortBy
                    }
                    if(sortOrder != "") {
                      queryList += "sortOrder=" + sortOrder
                    }
                    if(includeAllVersion != "") {
                      queryList += "includeAllVersions=" + includeAllVersion
                    }

                    val queryString = queryList.mkString("&")
                    feeder += Map("query" -> queryString)
                  }
                }
              }
            }
          }
        }
      }
    }

    feeder.toArray
  }

  def searchScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Search")
      .repeat(1000) {
        feed(this.searchQueryFeeder().circular)
          .exec(http("RegistryAPI.search")
            .get("""/api/-/search?${query}""")
            .headers(headers()))
        //          .check(status.is(200)))
      }
  }

  def verifyTokenScenario(): ScenarioBuilder = {
    scenario("RegistryAPI: Verify PAT")
      .repeat(1000) {
        feed(csv("namespaces.csv").circular)
          .feed(csv("access-tokens.csv").circular)
          .exec(http("RegistryAPI.verifyToken")
            .get("""/api/${namespace}/verify-pat?token=${access_token}""")
            .headers(headers())
            .requestTimeout(3.minutes))
      }
  }

  def extensionQueryScenario(): ScenarioBuilder = {
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

    scenario("VSCodeAdapter: Extension Query")
      .repeat(1000) {
        feed(csv("adapter/queries-xl.csv").circular)
          .exec(http("VSCodeAdapter.extensionQuery")
            .post(s"/vscode/gallery/extensionquery")
            .headers(headers())
            .body(StringBody(buildRequestBody)).asJson
            .requestTimeout(3.minutes)
            .check(status.is(200)))
      }
  }

  def vspackageScenario(): ScenarioBuilder = {
    scenario("VSCodeAdapter: Download")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .exec(http("VSCodeAdapter.download")
            .get("""/vscode/gallery/publishers/${namespace}/vsextensions/${name}/${version}/vspackage""")
            .headers(headers())
            .requestTimeout(3.minutes)
            .check(status.is(200)))
      }
  }

  def itemScenario(): ScenarioBuilder = {
    scenario("VSCodeAdapter: Get Item URL")
      .repeat(1000) {
        feed(csv("extensions.csv").circular)
          .exec(http("VSCodeAdapter.getItemUrl")
            .get("""/vscode/item?itemName=${namespace}.${name}""")
            .headers(headers())
            .requestTimeout(3.minutes)
            .check(status.is(200)))
      }
  }

  def unpkgScenario(): ScenarioBuilder = {
    scenario("VSCodeAdapter: Browse")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .feed(Array(Map("file" -> "extension.vsixmanifest"), Map("file" -> "extension")).circular)
          .exec(http("VSCodeAdapter.browse")
            .get("""/vscode/unpkg/${namespace}/${name}/${version}/${file}""")
            .headers(headers())
            .requestTimeout(3.minutes)
            .check(status.is(200)))
      }
  }

  def getAssetScenario(): ScenarioBuilder = {
    scenario("VSCodeAdapter: Get Asset")
      .repeat(1000) {
        feed(csv("extension-versions.csv").circular)
          .feed(Array(
            Map("asset" -> "Microsoft.VisualStudio.Services.Icons.Default"),
            Map("asset" -> "Microsoft.VisualStudio.Services.Content.Details"),
            Map("asset" -> "Microsoft.VisualStudio.Services.Content.Changelog"),
            Map("asset" -> "Microsoft.VisualStudio.Code.Manifest"),
            Map("asset" -> "Microsoft.VisualStudio.Services.VSIXPackage"),
            Map("asset" -> "Microsoft.VisualStudio.Services.Content.License"),
            Map("asset" -> "Microsoft.VisualStudio.Code.WebResources/extension/package.json")
          ).circular)
          .exec(http("VSCodeAdapter.getAsset")
            .get("""/vscode/asset/${namespace}/${name}/${version}/${asset}""")
            .headers(headers())
            .requestTimeout(3.minutes)
            .check(status.is(200)))
      }
  }
}
