package com.github.jw3.geo

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.github.jw3.geo.Api.{Commands, Events}
import geotrellis.vector.{Geometry, Point}

object Device {
  def props(id: String) = Props(new Device(id))

  case class Snapshot(position: Point)
}

class Device(id: String) extends PersistentActor with ActorLogging {
  val persistenceId: String = id
  var position: Option[Geometry] = None

  def receiveRecover: Receive = {
    case RecoveryCompleted ⇒
      log.info(s"device {} restored", id)

    case SnapshotOffer(_, ss: Device.Snapshot) ⇒
      log.info("restoring device {} to {}", id, ss.position)
      self ! Events.PositionUpdate(id, ss.position)

    case Events.DeviceAdded(id) ⇒
      log.debug(s"added event for $id")
      self ! Events.DeviceAdded(id)
  }

  def receiveCommand: Receive = {
    //
    // commands
    //
    case Commands.MoveDevice(_, g) ⇒
      val replyto = sender()
      persist(Events.PositionUpdate(id, g)) { e ⇒
        DeviceManager.device(id).foreach(_ ! e)
        replyto ! e
      }

    //
    // events
    //
    case e @ Events.PositionUpdate(mid, _) if mid == id ⇒
      DeviceManager.device(id).foreach(_ ! e)
  }
}
