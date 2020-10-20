package com.github.jw3.geo

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.jw3.BuildInfo
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Boot
    extends App
    with BootUtils
    with DeviceRoutes
    with EventRoutes
    with FenceRoutes
    with GeoDatabase
    with DataRoutes
    with LazyLogging {

  val config = pickConfig()
  logger.whenInfoEnabled { logger.info(banner(config)) }

  //
  // start the system

  implicit val system: ActorSystem = ActorSystem("geoserver", config)
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = 10 seconds

  //
  // connect readside

  val db = initdb(config) match {
    case Success(v) ⇒ v
    case Failure(ex) ⇒
      throw new RuntimeException("failed to connect readside db", ex)
  }

  val journaler = system.actorOf(Journaler.props(db), "journal")
  val deviceReadSide = system.actorOf(DeviceReadSide.props(), "device-read-side")

  //
  // start top level components

  val devices = system.actorOf(DeviceManager.props(), "devices")
  val fencing = system.actorOf(Fencer.props(), "fencer")

  //
  // start http

  val iface = "0.0.0.0"
  val routes = deviceRoutes(devices) ~ eventRoutes(deviceReadSide) ~ dataRoutes(db) ~ fenceRoutes(fencing)

  val httpEnabled = config.as[Boolean]("geo.http.enabled")
  if (httpEnabled) {
    val port = config.as[Int]("geo.http.port")
    logger.info(s"starting http on $iface:$port")
    Http().bindAndHandle(routes, iface, port)
  }

  httpsConfig(config) match {
    case Some(cfg) ⇒
      logger.debug("enabling as-server SSL using keystore@[{}]", cfg.ks)
      val port = config.as[Int]("geo.https.port")
      logger.info(s"starting https on $iface:$port")
      Http().bindAndHandle(routes, iface, port, connectionContext = ssl.from(cfg.ks, cfg.pass))

    case None if !httpEnabled ⇒
      logger.warn("neither http or https were enabled")

    case _ ⇒
      logger.warn("https is disabled")
  }
}

trait BootUtils {
  def pickConfig(): Config = {
    import ConfigFactory.load

    val cfg = load("application.conf")
    if (cfg.as[Boolean]("geo.db.ephemeral"))
      load("persistence-memory-app.conf")
    else
      load("persistence-postgres-app.conf")
  }

  case class HttpsConfig(ks: Path, pass: Array[Char])
  def httpsConfig(config: Config): Option[HttpsConfig] = {
    (
      config.getAs[Boolean]("geo.https.enabled"),
      config.getAs[String]("geo.https.ks.path"),
      config.getAs[String]("geo.https.ks.pass")
    ) match {
      case (Some(true), Some(path), Some(pass)) ⇒
        Some(HttpsConfig(Paths.get(path), pass.toCharArray))
      case _ ⇒ None
  }}

  def banner(cfg: Config): String = {
    val httpEnabled = cfg.as[Boolean]("geo.http.enabled")
    val httpsEnabled = httpsConfig(cfg).isDefined
    s"""
        |
        |       ,---.
        |    ,.'-.   \\
        |   ( ( ,'\"\"\"\"\"-.
        |   `,X          `.
        |   /` `           `._
        |  (            ,   ,_\\
        |  |          ,---.,'o `.
        |  |         / o   \\     )
        |   \\ ,.    (      .____,
        |    \\| \\    \\____,'     \\
        |  '`'\\  \\        _,____,'
        |  \\  ,--      ,-'     \\
        |    ( C     ,'         \\
        |     `--'  .'           |
        |       |   |         .O |     doh v${BuildInfo.version}
        |     __|    \\        ,-'_
        |    / `L     `._  _,'  ' `.
        |   /    `--.._  `',.   _\\  `
        |   `-.       /\\  | `. ( ,\\  \\
        |  _/  `-._  /  \\ |--'  (     \\
        | '  `-.   `'    \\/\\`.   `.    )
        |       \\  -hrr-    \\ `.  |    |
        |
        |
        |geoevents server
        |===============
        |
        | postgis db:    ${cfg.as[String]("slick.db.name")}
        | postgis host:  ${cfg.as[String]("slick.db.host")}
        | http port:     ${if (httpEnabled) cfg.as[Int]("geo.http.port") else "Disabled"}
        | https port:    ${if (httpsEnabled) cfg.as[Int]("geo.https.port") else "Disabled"}
        |
      """.stripMargin
  }
}
