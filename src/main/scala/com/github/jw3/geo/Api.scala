package com.github.jw3.geo

import geotrellis.vector.{Point, Polygon}
import geotrellis.vector.io.json.Implicits._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object Api {
  sealed trait Command
  sealed trait Event

  object Events extends DefaultJsonProtocol {
    case class DeviceAdded(id: String) extends Event

    case class PositionUpdate(device: String, pos: Point) extends Event
    object PositionUpdate {
      implicit val format: RootJsonFormat[PositionUpdate] = jsonFormat2(PositionUpdate.apply)
    }

    case class FencingCreated(name: String, geom: Polygon)
  }

  object Commands extends DefaultJsonProtocol {
    case class AddDevice(id: String) extends Command
    case class MoveDevice(device: String, geom: Point) extends Command
    case class AddFencing(name: String, geom: Polygon) extends Command
  }
}
