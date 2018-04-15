package com.github.jw3

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

object Boot extends App with GeoRoutes with LazyLogging {
  implicit val system = ActorSystem("geoserver")
  implicit val mat = ActorMaterializer()
  val config = system.settings.config

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  logger.info(s"starting http on $iface:$port")
  Http().bindAndHandle(routes, iface, port)
}

trait GeoRoutes {
  import akka.http.scaladsl.server.Directives._

  def routes =
    pathPrefix("api") {
      path("health") {
        get {
          complete(StatusCodes.OK)
        }
      }
    }
}
