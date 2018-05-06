package com.github.jw3.geo

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.github.jw3.geo.Api.{Commands, Events, Responses}
import geotrellis.vector.Point

object Device {
  def props(id: String) = Props(new Device(id))

  case class Snapshot(position: Point)
}

class Device(id: String) extends PersistentActor with ActorLogging {
  val persistenceId: String = id
  var position: Option[Point] = None
  var tracking: Option[String] = None

  def receiveRecover: Receive = {
    case RecoveryCompleted ⇒
      log.debug(s"device {} created", id)

    case SnapshotOffer(_, ss: Device.Snapshot) ⇒
      log.debug("snap device {} to {}", id, ss.position)
      position = Some(ss.position)

    case Events.PositionUpdate(_, pos) ⇒
      log.debug("replayed device move {}", pos)
      position = Some(pos)
  }

  def receiveCommand: Receive = {
    //
    // commands
    //
    case Commands.MoveDevice(_, g) ⇒
      persist(Events.PositionUpdate(id, g)) { e ⇒
        //
      }

    case Commands.StartTracking(`id`) if tracking.isEmpty ⇒
      persist(Events.TrackStarted(UUID.randomUUID.toString.take(8), id)) { e ⇒
        tracking = Some(e.id)
      }

    case Commands.StopTracking(`id`) if tracking.nonEmpty ⇒
      persist(Events.TrackCompleted(tracking.get, id)) { e ⇒
        tracking = None
      }

    //
    // events
    //
    case e @ Events.PositionUpdate(`id`, _) ⇒
    //
    // read-only commands
    //
    case Commands.GetDevicePosition(`id`) ⇒
      position match {
        case None ⇒ sender ! Responses.UnknownDevicePosition(id)
        case Some(p) ⇒ sender ! Responses.DevicePosition(id, p)
      }
  }
}
