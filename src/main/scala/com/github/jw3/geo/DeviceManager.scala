package com.github.jw3.geo

import akka.actor.{ActorContext, ActorLogging, ActorRef, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.github.jw3.geo.Api.Commands.TrackingCommand
import com.github.jw3.geo.Api.{Commands, Events, Queries, Responses}
import com.github.jw3.geo.DeviceManager._

object DeviceManager {
  def props() = Props(new DeviceManager)
  private def device(id: String)(implicit ctx: ActorContext): Option[ActorRef] = ctx.child(id)

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
      log.debug(s"device manager restored via persistence")

    case SnapshotOffer(_, ss: DeviceManager.Snapshot) ⇒
      // todo;; this is not firing
      log.debug("restoring {} devices", ss.devices.size)
      ss.devices.foreach(self ! Events.DeviceAdded(_))

    case Events.DeviceAdded(id) ⇒
      log.info(s"added event for $id")
      self ! Events.DeviceAdded(id)
  }

  def receiveCommand: Receive = {
    //
    // commands
    //
    case Commands.AddDevice(id, _) ⇒
      val replyto = sender()
      device(id) match {
        case Some(_) ⇒ replyto ! Responses.DeviceExists(id)
        case None ⇒
          val ref = context.actorOf(Device.props(id), id)
          persist(Events.DeviceAdded(id)) { e ⇒
            replyto ! e
            log.info("device added [{}]", ref.path.name)
          }
      }

    //
    // queries
    //
    case DeviceManager.QueryDeviceStatus(id) ⇒
      device(id) match {
        case None ⇒ sender ! DeviceManager.DeviceStatusUnknown
        case Some(_) ⇒ sender ! DeviceManager.DeviceOnline
      }

    case q @ Queries.GetDevicePosition(id) ⇒
      device(id) match {
        case Some(ref) => ref forward q
        case None ⇒ sender ! Responses.UnknownDevicePosition
      }

    //
    // forwarded
    //
    case cmd @ Commands.MoveDevice(id, _) ⇒
      device(id) match {
        case Some(d) ⇒ d forward cmd
        case None ⇒
          val ref = context.actorOf(Device.props(id), id)
          persist(Events.DeviceAdded(id)) { e ⇒
            log.info("device added (implicit move) [{}]", ref.path.name)
            ref forward cmd
          }
      }

    case cmd @ Commands.HeartBeat(id) ⇒
      device(id) match {
        case Some(d) ⇒ d forward cmd
        case None ⇒
          val ref = context.actorOf(Device.props(id), id)
          persist(Events.DeviceAdded(id)) { e ⇒
            log.info("device added (implicit heartbeat) [{}]", ref.path.name)
            ref forward cmd
          }
      }

    case c: TrackingCommand ⇒
      device(c.device).foreach(_ forward c)
  }
}
