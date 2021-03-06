package com.github.jw3.geo

import java.time.{LocalDateTime, ZoneOffset}

import geotrellis.vector.io.json.Implicits._
import geotrellis.vector.{Geometry, Point, Polygon}
import spray.json._

object Api {
  sealed trait Command
  sealed trait Event
  sealed trait Response[_ <: Command]

  // com.github.jw3.geo.Api$Events$
  object Events extends DefaultJsonProtocol {
    case class DeviceAdded(id: String) extends Event

    sealed trait TrackingEvent

    case class TrackStarted(id: String, device: String, beginSeqNr: Long, beginPt: Point)
        extends Event
        with TrackingEvent

    case class TrackCompleted(id: String,
                              device: String,
                              beginSeqNr: Long,
                              endSeqNr: Long,
                              beginPt: Point,
                              endPt: Point)
        extends Event
        with TrackingEvent

    case class TrackCancelled(id: String, beginPt: Point, endPt: Point) extends Event with TrackingEvent

    case class PositionUpdate(device: String, pos: Point, when: LocalDateTime) extends Event {
      def asTxtMsg() = s"$device:${pos.x}:${pos.y}:${when.seconds}"
    }
    object PositionUpdate {
      implicit val format: RootJsonFormat[PositionUpdate] = jsonFormat3(PositionUpdate.apply)
    }

    case class FencingChange(id: String, device: String, state: String)
    object FencingChange {
      val Entered = "ENTER"
      val Exited = "EXIT"

      def enter(id: String, dev: String) = FencingChange(id, dev, Entered)
      def exit(id: String, dev: String) = FencingChange(id, dev, Exited)

      implicit val format: RootJsonFormat[FencingChange] = jsonFormat3(FencingChange.apply)
    }

    sealed trait FenceContainment {
      def id: String
      def name: String
      def device: String
    }
    case class EnteredFence(id: String, name: String, device: String) extends FenceContainment
    case class ExitedFence(id: String, name: String, device: String) extends FenceContainment
    case class FenceCreated(fid: String, name: String, geom: Polygon)
  }

  object Commands extends DefaultJsonProtocol {
    case class AddDevice(id: String, version: Option[String]) extends Command
    case class Disconnected(device: String) extends Command
    case class MoveDevice(device: String, geom: Point) extends Command
    case class HeartBeat(id: String)
    case object Nop extends Command

    sealed trait TrackingCommand {
      def device: String
    }
    case class StartTracking(device: String) extends Command with TrackingCommand
    case class StopTracking(device: String) extends Command with TrackingCommand

    case class CreateFence(name: String, geom: Geometry) extends Command
  }

  object Queries extends DefaultJsonProtocol {
    case class GetDevicePosition(id: String) extends Command
  }

  object Responses extends DefaultJsonProtocol {
    case class DeviceExists(id: String) extends Response[Commands.AddDevice]
    case class UnknownDevicePosition(id: String) extends Response[Queries.GetDevicePosition]
    case class DevicePosition(id: String, geom: Point) extends Response[Queries.GetDevicePosition]
    case class DeviceRemoved(id: String) extends Response[Commands.Disconnected]
    case class DeviceNotConnected(id: String)

    case class InvalidFence()
  }

  case class FenceData(name: String)
  object FenceData extends DefaultJsonProtocol {
    implicit val format: RootJsonFormat[FenceData] = jsonFormat1(FenceData.apply)
  }

  object Tags {
    val Movement = "movement"
    val Tracks = "tracks"
    val Fencing = "fencing"
  }

  private implicit val localDateTimeFormat: RootJsonFormat[LocalDateTime] = new RootJsonFormat[LocalDateTime] {
    def read(json: JsValue): LocalDateTime = json match {
      case JsNumber(v) ⇒ LocalDateTime.ofEpochSecond(v.toIntExact, 0, ZoneOffset.UTC)
      case v ⇒ deserializationError(s"expected JsNumber, got $v")
    }

    def write(dt: LocalDateTime): JsValue = JsNumber(dt.getSecond)
  }

  implicit class RichLocalDateTime(dt: LocalDateTime) {
    def seconds: Long = dt.toEpochSecond(ZoneOffset.UTC)
  }
}
