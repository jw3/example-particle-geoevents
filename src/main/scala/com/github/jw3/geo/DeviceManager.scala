package com.github.jw3.geo

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.github.jw3.geo.Api.{Commands, Events}
import geotrellis.vector.Geometry

object DeviceManager {
  def props() = Props(new DeviceManager)

  def device(id: String)(implicit context: ActorContext): ActorRef = {
    context.child(id) match {
      case Some(ref) ⇒ ref
      case None ⇒ context.actorOf(Device.props(), id)
    }
  }
}

class DeviceManager extends PersistentActor with ActorLogging {
  def persistenceId: String = "device-manager"

  def receiveRecover: Receive = {
    case _ ⇒
  }

  def receiveCommand: Receive = {
    case Commands.MoveDevice(id, g) ⇒
      persist(Events.PositionUpdate(id, g)) { e ⇒
        DeviceManager.device(id) ! e
      }

    case e @ Events.PositionUpdate(id, _) ⇒
      DeviceManager.device(id) ! e
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
