package com.github.jw3.geo

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.jw3.geo.Api.HookCall
import com.typesafe.scalalogging.LazyLogging
import geotrellis.vector.Point
import net.ceedubs.ficus.Ficus._

object Boot extends App with GeoRoutes with LazyLogging {
  implicit val system = ActorSystem("geoserver")
  implicit val mat = ActorMaterializer()
  val config = system.settings.config

  val iface = "0.0.0.0"
  val port = config.as[Int]("geo.http.port")

  val fencing = system.actorOf(Fencer.props(), "fencing")

  logger.info(s"starting http on $iface:$port")
  Http().bindAndHandle(routes(fencing), iface, port)
}

trait GeoRoutes {
  import akka.http.scaladsl.server.Directives._
  import Api.Formats._

  def routes(fencing: ActorRef): Route =
    pathPrefix("api") {
      path("move") {
        post {
          entity(as[HookCall]) { e ⇒
            import geotrellis.vector.io.json.Implicits._

            // e.data = "34.12345:-79.09876"
            val xy = e.data.split(":")
            val pt = Point(xy(0).toDouble, xy(1).toDouble)
            complete(pt)
          }
        }
      } ~
        path("fence") {
          post {
            complete(StatusCodes.OK)
          }
        } ~
        path("health") {
          get {
            complete(StatusCodes.OK)
          }
        }
    }
}
