package com.github.jw3.geo

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.persistence.query.EventEnvelope
import akka.stream.Materializer
import com.github.jw3.geo.Api.Events.{EnteredFence, ExitedFence, PositionUpdate}
import com.github.jw3.geo.Fence.{Inside, Outside, fenceId}
import geotrellis.vector.Polygon

object Fence {
  def props(name: String, geom: Polygon)(implicit mat: Materializer) = Props(new Fence(name, geom))
  def fenceId(ref: ActorRef): String = ref.path.name

  private case class Inside(dev: String)
  private case class Outside(dev: String)
}

class Fence(name: String, geom: Polygon)(implicit mat: Materializer) extends Actor with ActorLogging {
  import context._

  val fid = fenceId(self)

  // todo;; persist offset
  Streams.movement().runForeach {
    case EventEnvelope(_, _, _, PositionUpdate(dev, pos)) ⇒
      val in = geom.contains(pos)
      if (in)
        self ! Inside(dev)
      else
        self ! Outside(dev)
    case _ ⇒
  }.foreach(self ! _)

  def handle(in: List[String]): Receive = {
    case Inside(dev) if !in.contains(dev) ⇒
      parent ! EnteredFence(fid, name, dev)
      println(s"$dev moved into $name")
      become(handle(in :+ dev))

    case Outside(dev) if in.contains(dev) ⇒
      parent ! ExitedFence(fid, name, dev)
      println(s"$dev moved out of $name")
      become(handle(in.filterNot(_ == dev)))

    case Done ⇒
      println("******")
  }

  // todo;; restore from persistence
  def receive: Receive = handle(List.empty)
}
