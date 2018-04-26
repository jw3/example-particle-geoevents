package com.github.jw3.geo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext.Implicits.global
import slick.jdbc.PostgresProfile.api._

import scala.util.{Failure, Success}

object Boot extends App with GeoRoutes with GeoDatabase with LazyLogging {
  val config = pickConfig()

  implicit val system: ActorSystem = ActorSystem("geoserver", config)
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val db = initdb()
  db.foreach { db ⇒
    import GeoConcepts.EventTable.events

    val num = 10
    val f = db.run(events.take(num).result)
    f.onComplete {
      case Success(r) ⇒ logger.info(s"connected to db")
      case Failure(ex) ⇒ logger.error(s"failed to query last $num events")
    }
  }

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  val devices = system.actorOf(DeviceManager.props(), "devices")
  val fencing = system.actorOf(Fencer.props(), "fencing")

  logger.info(s"starting http on $iface:$port")
  Http().bindAndHandle(routes(devices, fencing), iface, port)

  def pickConfig(): Config = {
    import ConfigFactory.load

    val cfg = load()
    if (cfg.as[Boolean]("geo.db.ephemeral"))
      cfg.withFallback(load("persistence-memory.conf"))
    else
      cfg.withFallback(load("persistence-postgres.conf"))
  }
}
