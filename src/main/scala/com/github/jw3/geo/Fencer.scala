package com.github.jw3.geo

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.github.jw3.geo.Fencer.{FeatureCreated, FromGeoJson}
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.Implicits._
import geotrellis.vector.{Geometry, Polygon}

object Fencer {
  def props() = Props(new Fencer)

  final case class FromGeoJson(name: String, json: String)
  final case class FeatureCreated(name: String, fid: String)
}

class Fencer extends Actor with ActorLogging {
  def receive: Receive = {
    case FromGeoJson(name, json) ⇒
      val geom = GeoJson.parse[Geometry](json)
      val fid = UUID.randomUUID.toString.take(8)

      geom match {
        case g @ Polygon(_) ⇒
          context.actorOf(Feature.area(name, g), fid)
          sender ! FeatureCreated(name, fid)
      }
  }
}
