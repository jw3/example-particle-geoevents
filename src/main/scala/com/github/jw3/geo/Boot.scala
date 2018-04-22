package com.github.jw3.geo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

object Boot extends App with GeoRoutes with GeoDatabase with LazyLogging {
  implicit val system = ActorSystem("geoserver")
  implicit val mat = ActorMaterializer()
  val config = system.settings.config

  val db = initdb()

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  val fencing = system.actorOf(Fencer.props(), "fencing")

  logger.info(s"starting http on $iface:$port")
  Http().bindAndHandle(routes(fencing), iface, port)
}
