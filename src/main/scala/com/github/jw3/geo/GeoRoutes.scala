package com.github.jw3.geo

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.github.jw3.geo.Api.HookCall
import geotrellis.vector.Point
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

trait GeoRoutes {
  import akka.http.scaladsl.server.Directives._

  def routes(fencing: ActorRef): Route =
    extractLog { logger ⇒
      pathPrefix("api") {
        path("move") {
          post {
            entity(as[HookCall]) { e ⇒
              import geotrellis.vector.io.json.Implicits._
              logger.info(s"move [${e.coreid}] [${e.data}]")

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
}
