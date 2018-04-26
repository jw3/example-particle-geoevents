package com.github.jw3.geo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object Boot extends App with BootUtils with GeoRoutes with GeoDatabase with LazyLogging {
  val config = pickConfig()

  //
  // start the system

  implicit val system: ActorSystem = ActorSystem("geoserver", config)
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = 10 seconds;

  //
  // connect readside

  initdb() match {
    case Some(db) ⇒
      system.actorOf(Journaler.props(db), "journal")
    case None ⇒
      logger.error("failed to connect readside db")
  }

  //
  // start top level components

  val devices = system.actorOf(DeviceManager.props(), "devices")
  val fencing = system.actorOf(Fencer.props(), "fencing")

  //
  // start http

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  logger.info(s"starting http on $iface:$port")
  Http().bindAndHandle(routes(devices, fencing), iface, port)
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
}
