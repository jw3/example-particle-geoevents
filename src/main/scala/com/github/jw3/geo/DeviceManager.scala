package com.github.jw3.geo

import akka.actor.{ActorContext, ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.github.jw3.geo.Api.{Commands, Events}

object DeviceManager {
  def props() = Props(new DeviceManager)
  def device(id: String)(implicit ctx: ActorContext): Option[ActorRef] = ctx.child(id)

  case class Snapshot(devices: Set[String])

  sealed trait DeviceStatus
  object DeviceStatusUnknown extends DeviceStatus
  object DeviceOnline extends DeviceStatus

  sealed trait Query
  case class QueryDeviceStatus(id: String) extends Query
}

class DeviceManager extends PersistentActor with ActorLogging {
  val persistenceId: String = "device-manager"

  def receiveRecover: Receive = {
    case RecoveryCompleted ⇒
      log.info(s"device manager restored via persistence")

    case SnapshotOffer(_, ss: DeviceManager.Snapshot) ⇒
      log.info("restoring {} devices", ss.devices.size)
      ss.devices.foreach(self ! Events.DeviceAdded(_))

    case Events.DeviceAdded(id) ⇒
      log.debug(s"added event for $id")
      self ! Events.DeviceAdded(id)
  }

  def receiveCommand: Receive = {
    //
    // commands
    //
    case Commands.AddDevice(id) ⇒
      val replyto = sender()
      persist(Events.DeviceAdded(id)) { e ⇒
        self ! e
        replyto ! e
      }

    //
    // events
    //
    case Events.DeviceAdded(id) ⇒
      val ref = context.actorOf(Device.props(id), id)
      log.debug("created device [{}]", ref.path.name)

    //
    // queries
    //
    case DeviceManager.QueryDeviceStatus(id) ⇒
      context.child(id) match {
        case None ⇒ sender ! DeviceManager.DeviceStatusUnknown
        case Some(_) ⇒ sender ! DeviceManager.DeviceOnline
      }

    //
    // read-only commands
    //
    case c @ Commands.MoveDevice(id, _) ⇒
      context.child(id).foreach(_ forward c)
  }
}
