package com.github.jw3.geo

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.github.jw3.geo.Api.HookCall
import geotrellis.vector.Point

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
