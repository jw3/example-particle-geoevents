package com.github.jw3.geo

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

    //
    // events
    //
    case e @ Events.PositionUpdate(_id, _) if _id == id ⇒

    //
    // read-only commands
    //
    case Commands.GetDevicePosition(_id) if _id == id ⇒
      position match {
        case None ⇒ sender ! Responses.UnknownDevicePosition(id)
        case Some(p) ⇒ sender ! Responses.DevicePosition(id, p)
      }
  }
}
