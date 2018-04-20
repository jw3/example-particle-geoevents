package com.github.jw3.geo

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import julienrf.json
import play.api.libs.json.Format
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object Api {
  sealed trait HookEvent {
    def id: String
  }

  final case class HookCall(name: String, data: String, coreid: String, published_at: String)
  object HookCall {
    implicit val reads: Format[HookCall] = json.derived.oformat()
  }

  final case class Moved(id: String, source: String, lat: String, lon: String) extends HookEvent

  object Formats extends DefaultJsonProtocol with SprayJsonSupport {
    implicit def p2sFormat[T](implicit playFormat: Format[T]): RootJsonFormat[T] = new RootJsonFormat[T] {
      def read(json: spray.json.JsValue): T = play.api.libs.json.Json.parse(json.compactPrint).as[T]
      def write(obj: T): JsValue = spray.json.enrichString(play.api.libs.json.Json.toJson(obj).toString).parseJson
    }
  }
}
