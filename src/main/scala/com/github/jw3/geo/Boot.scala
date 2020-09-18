package com.github.jw3.geo

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

  //
  // start top level components

  val devices = system.actorOf(DeviceManager.props(), "devices")
  val fencing = system.actorOf(Fencer.props(), "fencing")

  //
  // start http

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  logger.info(s"starting http on $iface:$port")
  val routes = deviceRoutes(devices) ~ eventRoutes() ~ dataRoutes(db)
  Http().bindAndHandle(routes, iface, port)
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

  def banner(cfg: Config): String = {
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
        |
      """.stripMargin
  }
}
