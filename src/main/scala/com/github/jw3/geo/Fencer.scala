package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}
import com.github.jw3.geo.Fencer.{FeatureCreated, FromGeoJson}
import geotrellis.vector.Geometry
import geotrellis.vector.io.json.GeoJson
import geotrellis.vector.io.json.Implicits._

object Fencer {
  def props() = Props(new Fencer)

  final case class FromGeoJson(rid: String, json: String)
  final case class FeatureCreated(rid: String, fid: String)
}

class Fencer extends Actor with ActorLogging {
  def receive: Receive = {
    case FromGeoJson(rid, json) ⇒
      val geom = GeoJson.parse[Geometry](json)
      val fid = "some_generated_id"
      context.actorOf(Feature.props(fid, geom))
      sender ! FeatureCreated(rid, fid)
  }
}
