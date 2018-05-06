package com.github.jw3.geo

import geotrellis.vector.io.json.Implicits._
import geotrellis.vector.{Point, Polygon}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object Api {
  sealed trait Command
  sealed trait Event
  sealed trait Response[_ <: Command]

  // com.github.jw3.geo.Api$Events$
  object Events extends DefaultJsonProtocol {
    case class DeviceAdded(id: String) extends Event
    case class TrackStarted(id: String, device: String) extends Event
    case class TrackCompleted(id: String, device: String) extends Event

    case class PositionUpdate(device: String, pos: Point) extends Event
    object PositionUpdate {
      implicit val format: RootJsonFormat[PositionUpdate] = jsonFormat2(PositionUpdate.apply)
    }

    case class FencingCreated(name: String, geom: Polygon)
  }

  object Commands extends DefaultJsonProtocol {
    case class AddDevice(id: String) extends Command
    case class GetDevicePosition(id: String) extends Command
    case class MoveDevice(device: String, geom: Point) extends Command

    case class StartTracking(device: String) extends Command
    case class StopTracking(device: String) extends Command

    case class AddFencing(name: String, geom: Polygon) extends Command
  }

  object Responses extends DefaultJsonProtocol {
    case class DeviceExists(id: String) extends Response[Commands.AddDevice]
    case class UnknownDevicePosition(id: String) extends Response[Commands.GetDevicePosition]
    case class DevicePosition(id: String, geom: Point) extends Response[Commands.GetDevicePosition]
  }

  object Tags {
    val Events = "events"
    val Tracks = "tracks"
  }
}
