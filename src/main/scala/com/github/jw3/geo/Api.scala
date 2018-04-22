package com.github.jw3.geo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import geotrellis.vector.Point
import julienrf.json.derived
import play.api.libs.json.Format
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object Api {
  sealed trait UserEvent
  final case class FenceAdded() extends UserEvent

  sealed trait DeviceEvent {
    def device: String
  }
  object DeviceEvent {
    implicit val format: Format[DeviceEvent] = derived.oformat()
  }

  final case class Moved(device: String, pos: Point) extends DeviceEvent
  object Moved {
    implicit val format: Format[Moved] = derived.oformat()
  }

  final case class HookCall(name: String, data: String, coreid: String, published_at: String)
  object HookCall {
    implicit val format: Format[HookCall] = derived.oformat()
  }

  object Formats extends DefaultJsonProtocol with SprayJsonSupport {
    implicit def p2sFormat[T](implicit playFormat: Format[T]): RootJsonFormat[T] = new RootJsonFormat[T] {
      def read(json: spray.json.JsValue): T = play.api.libs.json.Json.parse(json.compactPrint).as[T]
      def write(obj: T): JsValue = spray.json.enrichString(play.api.libs.json.Json.toJson(obj).toString).parseJson
    }
  }
}
