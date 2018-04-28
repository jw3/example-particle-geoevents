package com.github.jw3.geo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Boot extends App with BootUtils with DeviceRoutes with EventRoutes with GeoDatabase with LazyLogging {
  val config = pickConfig()

  //
  // start the system

  implicit val system: ActorSystem = ActorSystem("geoserver", config)
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = 10 seconds

  //
  // connect readside

  val journaler = initdb(config) match {
    case Success(db) ⇒
      system.actorOf(Journaler.props(db), "journal")
    case Failure(ex) ⇒
      throw new RuntimeException("failed to connect readside db", ex)
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
  val routes = deviceRoutes(devices, fencing) ~ eventRoutes(journaler)
  Http().bindAndHandle(routes, iface, port)

  //
  // log event stream
  logger.whenDebugEnabled {
    Streams.readJournal.runWith(Sink.foreach(ee ⇒ logger.debug("{}", ee)))
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
}
