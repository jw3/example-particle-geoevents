package com.github.jw3.geo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import geotrellis.vector.Point
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import geotrellis.vector.io.json.Implicits._

object Api {
  sealed trait UserEvent
  final case class FenceAdded() extends UserEvent

  sealed trait DeviceEvent {
    def device: String
  }

  final case class Moved(device: String, pos: Point) extends DeviceEvent
  object Moved extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val format: RootJsonFormat[Moved] = jsonFormat2(Moved.apply)
  }

  final case class HookCall(event: String, data: String, coreid: String, published_at: String)
  object HookCall extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val format: RootJsonFormat[HookCall] = jsonFormat4(HookCall.apply)
  }
}
