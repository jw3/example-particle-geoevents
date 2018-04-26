package com.github.jw3.geo

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.github.jw3.geo.Api.{Commands, Events}
import geotrellis.vector.Geometry

object DeviceManager {
  def props() = Props(new DeviceManager)
  def device(id: String)(implicit ctx: ActorContext): Option[ActorRef] = ctx.child(id)

  sealed trait DeviceStatus
  object DeviceStatusUnknown extends DeviceStatus
  object DeviceOnline extends DeviceStatus

  sealed trait Query
  case class QueryDeviceStatus(id: String) extends Query
}

class DeviceManager extends PersistentActor with ActorLogging {
  def persistenceId: String = "device-manager"

  def receiveRecover: Receive = {
    case _ ⇒
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

    case Commands.MoveDevice(id, g) ⇒
      val replyto = sender()
      persist(Events.PositionUpdate(id, g)) { e ⇒
        DeviceManager.device(id).foreach(_ ! e)
        replyto ! e
      }

    //
    // events
    //

    case Events.DeviceAdded(id) ⇒
      val ref = context.actorOf(Device.props(), id)
      log.debug("created device [{}]", ref.path.name)

    case e @ Events.PositionUpdate(id, _) ⇒
      DeviceManager.device(id).foreach(_ ! e)

    //
    // queries
    //
    case DeviceManager.QueryDeviceStatus(id) ⇒
      context.child(id) match {
        case None ⇒ sender ! DeviceManager.DeviceStatusUnknown
        case Some(_) ⇒ sender ! DeviceManager.DeviceOnline
      }
  }
}

object Device {
  def props() = Props(new DeviceManager)
}

class Device extends Actor with ActorLogging {
  def positioned(pos: Option[Geometry]): Receive = {
    case Events.PositionUpdate(_, g) ⇒
      context.become(positioned(Some(g)))
  }

  def receive: Receive = positioned(None)
}
