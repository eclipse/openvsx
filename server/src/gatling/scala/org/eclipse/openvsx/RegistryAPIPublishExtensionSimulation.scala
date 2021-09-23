package org.eclipse.openvsx

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.nio.file.Files
import scala.concurrent.duration.DurationInt
import scala.reflect.io.File

class RegistryAPIPublishExtensionSimulation extends Simulation {

  def extensionFilesFeeder(extensionDir: String): Array[Map[String,String]] = {
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

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .disableCaching

  val extensionDir = "<EXTENSION_DIR>"
  val feeder = this.extensionFilesFeeder(extensionDir)

  val users = 4
  val repeatTimes = feeder.length / users
  val scn = scenario("RegistryAPI: Publish Extension")
    .repeat(repeatTimes) {
      feed(feeder)
        .feed(csv("access-tokens.csv").circular)
        .exec(http("publishExtension")
          .post("/api/-/publish")
          .queryParam("token", """${access_token}""")
          .body(ByteArrayBody(session => {
            val path = extensionDir + "\\" + session("extension_file").as[String]
            File(path).toByteArray()
          }))
          .requestTimeout(3.minutes)
          .check(status.is(201)))
    }

  setUp(scn.inject(atOnceUsers(users))).protocols(httpProtocol)

//  def downloadExtensions(extensionDir: String, users: Int): Unit = {
//    val httpProtocol = http
//      .baseUrl("https://open-vsx.org")
//      .disableCaching
//
//    val scn = scenario("RegistryAPI: Download Extension")
//      .repeat(744) {
//        feed(csv("extension-versions.csv"))
//          .feed(csv("access-tokens.csv").circular)
//          .exec(session => {
//            val namespace = session("namespace").as[String]
//            val name = session("name").as[String]
//            val version = session("version").as[String]
//            session.set("extension_file", s"$extensionDir\\$namespace-$name-$version.vsix")
//          })
//          .doIf(session => {
//            val extensionFile = session("extension_file").as[String]
//            println(extensionFile)
//            !File(extensionFile).exists
//          }) {
//            exec(http("getExtensionDownloadLink")
//              .get("""/api/${namespace}/${name}/${version}""")
//              .check(jsonPath("$.files.download").find.saveAs("download")))
//              .exec(http("downloadExtension")
//                .get("""${download}""")
//                .check(bodyBytes.saveAs("file_bytes")))
//              .pause(30, 60)
//              .exec {session =>
//                Files.write(new java.io.File(session("extension_file").as[String]).toPath, session("file_bytes").as[Array[Byte]])
//                session
//              }
//          }
//      }
//
//    setUp(scn.inject(atOnceUsers(users))).protocols(httpProtocol)
//  }
}
