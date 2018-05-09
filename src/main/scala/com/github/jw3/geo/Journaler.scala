package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.ActorMaterializer
import com.github.jw3.geo.Api.Events.{PositionUpdate, TrackCompleted}
import com.github.jw3.geo.GeoConcepts.EventTable.events
import com.github.jw3.geo.GeoConcepts.TrackTable.tracks
import com.github.jw3.geo.PgDriver.api._
import geotrellis.vector.Line

import scala.util.{Failure, Success}

/**
  * the read-side journaler
  */
object Journaler {
  def props(db: Database)(implicit mat: ActorMaterializer) = Props(new Journaler(db))
}

class Journaler(db: Database)(implicit mat: ActorMaterializer) extends Actor with ActorLogging {
  import context.{dispatcher, system}

  Streams.movement().runForeach {
    case EventEnvelope(_, _, _, PositionUpdate(dev, pos)) ⇒
      val action = events += ((0, dev, pos))
      db.run(action).onComplete(context.self ! _)
    case _ ⇒
  }

  Streams.tracking().runForeach {
    case EventEnvelope(_, _, _, TrackCompleted(id, dev, begin, end, pt1, pt2)) ⇒
      log.debug("completed track {} at {}", id, pt2)

      Streams
        .movement(Offset.sequence(begin))
        .takeWhile(_.sequenceNr != end)
        .map(_.event)
        .filter {
          case _: PositionUpdate ⇒ true
          case _ ⇒ false
        }
        .map(_.asInstanceOf[PositionUpdate])
        .fold(Line(pt1, pt2)) { (line, mv) ⇒
          Line(line.points.dropRight(1) :+ mv.pos :+ line.points.last: _*)
        }
        .runForeach { line ⇒
          log.debug("saving track for {} as {}", dev, line)
          val action = tracks += ((0, dev, begin, line))
          db.run(action).onComplete(context.self ! _)
        }

    case _ ⇒
  }

  def receive: Receive = {
    case Success(_) ⇒ log.debug("read-side journal success")
    case Failure(ex) ⇒ log.error(ex, "read-side journal failure")
  }
}
