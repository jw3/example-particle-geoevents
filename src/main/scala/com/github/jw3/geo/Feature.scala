package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import geotrellis.vector.{Geometry, Point, Polygon}

object Feature {
  def area(name: String, geom: Polygon) = Props(new AreaFeature(name, geom))

  def featureId(ref: ActorRef): String = ref.path.name

  case class ContainmentQuery(geom: Point)
  case class IntersectionQuery(geom: Geometry)
  case class BooleanResponse(value: Boolean)
}

class AreaFeature(name: String, geom: Polygon) extends Actor with ActorLogging {
  def receive: Receive = {
    case Feature.ContainmentQuery(pt) ⇒
      sender ! Feature.BooleanResponse(geom.contains(pt))
    case Feature.IntersectionQuery(g) ⇒
      sender ! Feature.BooleanResponse(geom.intersects(g))
  }
}
