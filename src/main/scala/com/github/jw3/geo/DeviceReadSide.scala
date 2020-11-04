package com.github.jw3.geo

import java.time.LocalDateTime

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.jw3.geo.Api.Events.PositionUpdate
import com.github.jw3.geo.DeviceReadSide.{DeviceMetadata, OffsetPositionUpdate, RequestStreamTo}
import geotrellis.vector.Point

object DeviceReadSide {
  def props()(implicit mat: ActorMaterializer): Props = Props(new DeviceReadSide)

  case class RequestStreamTo(sink: ActorRef)
  case class DeviceMetadata(where: Point, when: LocalDateTime)
  case class OffsetPositionUpdate(offset: Offset, what: String, where: Point, when: LocalDateTime)
}

class DeviceReadSide(implicit mat: ActorMaterializer) extends PersistentActor with ActorLogging {
  import context.system
  def persistenceId: String = "device-read-side"

  var metadata = Map.empty[String, DeviceMetadata]
  var moveOffset: Offset = Offset.sequence(0L)

  def receiveCommand: Receive = {
    case EventEnvelope(offset, _, _, e @ PositionUpdate(_, _, _)) ⇒
      persist(OffsetPositionUpdate(offset, e.device, e.pos, e.when)) { ee ⇒
        metadata += persistenceId → DeviceMetadata(ee.where, ee.when)
        moveOffset = offset
      }
    case RequestStreamTo(sink) ⇒
      Source
        .fromIterator(() ⇒ metadata.map(m ⇒ PositionUpdate(m._1, m._2.where, m._2.when)).iterator)
        .concat(Streams.movement(moveOffset).collect {
          case EventEnvelope(_, id, _, PositionUpdate(_, pos, t)) ⇒
            PositionUpdate(id, pos, t)
        })
        .runWith(Sink.actorRef(sink, akka.Done))
  }

  def receiveRecover: Receive = {
    case OffsetPositionUpdate(o, what, where, when) ⇒
      moveOffset = o
      metadata += what -> DeviceMetadata(where, when)

    case RecoveryCompleted ⇒
      log.info("starting from {}", moveOffset)
      Streams
        .movement(moveOffset)
        .filter {
          case EventEnvelope(_, _, _, PositionUpdate(_, _, _)) ⇒ true
          case _ ⇒ false
        }
        .runWith(Sink.actorRef(self, akka.Done))
  }
}
