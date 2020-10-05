package com.github.jw3.geo

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.Materializer
import com.github.jw3.geo.Api.Commands.CreateFence
import com.github.jw3.geo.Api.Events
import com.github.jw3.geo.Api.Responses.InvalidFence
import geotrellis.vector.Polygon

object Fencer {
  def props()(implicit mat: Materializer) = Props(new Fencer)
}

class Fencer(implicit mat: Materializer) extends Actor with ActorLogging {
  def receive: Receive = {
    case CreateFence(name, g @ Polygon(_)) ⇒
      val fid = UUID.randomUUID.toString.take(8)

      context.actorOf(Fence.props(name, g), fid)
      sender ! Events.FenceCreated(fid, name, g)

    case CreateFence(name, _) ⇒
      log.warning("invalid fence geometry, only polygons are supported")
      sender ! InvalidFence()
  }
}
