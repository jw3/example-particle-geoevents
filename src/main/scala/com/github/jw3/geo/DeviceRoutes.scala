package com.github.jw3.geo

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.github.jw3.geo.Api.Commands
import com.github.jw3.geo.Api.Commands.AddDevice
import com.github.jw3.geo.Api.Events.DeviceAdded
import com.github.jw3.geo.DeviceRoutes.HookCall
import geotrellis.vector.Point
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object DeviceRoutes {
  final case class HookCall(event: String, data: String, coreid: String, published_at: String)
  object HookCall extends DefaultJsonProtocol {
    implicit val format: RootJsonFormat[HookCall] = jsonFormat4(HookCall.apply)
  }
}

trait DeviceRoutes {
  import akka.http.scaladsl.server.Directives._

  def deviceRoutes(devices: ActorRef, fencing: ActorRef)(implicit to: Timeout): Route =
    extractLog { logger ⇒
      extractExecutionContext { implicit ec ⇒
        pathPrefix("api") {
          path("device" / Segment) { id ⇒
            post {
              val res = (devices ? AddDevice(id)).map {
                case DeviceAdded(_) ⇒ StatusCodes.OK
                case _ ⇒ StatusCodes.InternalServerError
              }
              complete(res)
            }
          } ~
            path("device" / Segment / "move") { id ⇒
              post {
                entity(as[HookCall]) { e ⇒
                  // e.data = "34.12345:-79.09876"
                  val xy = e.data.split(":")
                  val pt = Point(xy(0).toDouble, xy(1).toDouble)

                  devices ! Commands.MoveDevice(id, pt)
                  complete(StatusCodes.OK)
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
}
