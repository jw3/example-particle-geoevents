package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}
import geotrellis.vector.Geometry

object Feature {
  def props(id: String, geom: Geometry) = Props(new Feature(id, geom))
}

class Feature(id: String, geom: Geometry) extends Actor with ActorLogging {
  def receive: Receive = {
    case _ ⇒
  }
}
