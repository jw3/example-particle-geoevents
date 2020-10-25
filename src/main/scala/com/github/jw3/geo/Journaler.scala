package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.github.jw3.geo.Api.Events.{PositionUpdate, TrackCompleted}
import com.github.jw3.geo.Api.Tags
import com.github.jw3.geo.GeoConcepts.EventTable.events
import com.github.jw3.geo.GeoConcepts.TrackTable.tracks
import com.github.jw3.geo.Journaler.{MovePersisted, TrackPersisted}
import com.github.jw3.geo.OffsetStore.updateOffsetQuery
import com.github.jw3.geo.PgDriver.api._
import geotrellis.vector.{Line, Point}

import scala.util.{Failure, Success}

/**
  * the read-side journaler
  */
object Journaler {
  def props(db: Database)(implicit mat: ActorMaterializer) = Props(new Journaler(db))
  final val Id = "journaler"

  case class TrackPersisted(id: String)
  case class MovePersisted(device: String, pos: Point)
}

class Journaler(db: Database)(implicit mat: ActorMaterializer) extends Actor with ActorLogging {
  import context.{dispatcher, system}

  db.run(OffsetStore.getOffsetQuery(Journaler.Id, Tags.Movement)).flatMap { start ⇒
    // moves to child actor, MoveJournaler
    Streams.movement(start).runForeach {
      case EventEnvelope(offset, _, _, PositionUpdate(dev, pos, when)) ⇒
        val action = (events += ((0, dev, pos, when))).flatMap { _ ⇒
          updateOffsetQuery(Journaler.Id, Tags.Movement, offset)
        }
        db.run(action.transactionally).onComplete(_ ⇒ context.self ! Journaler.MovePersisted(dev, pos))

      case _ ⇒
    }
  }

  db.run(OffsetStore.getOffsetQuery(Journaler.Id, Tags.Tracks)).flatMap { start ⇒
    // moves to child actor, TrackJournaler
    Streams.tracking(start).runForeach {
      case EventEnvelope(offset, _, _, TrackCompleted(id, dev, begin, end, pt1, pt2)) ⇒
        log.info("completed track {} at {}", id, pt2)

        Streams
          .movement(Offset.sequence(begin))
          .takeWhile(_.sequenceNr != end)
          .map(_.event)
          .filter(_.isInstanceOf[PositionUpdate])
          .map(_.asInstanceOf[PositionUpdate])
          .fold(Line(pt1, pt2)) { (line, mv) ⇒
            Line(line.points.dropRight(1) :+ mv.pos :+ line.points.last: _*)
          }
          .log("journaler", line ⇒ s"saving track for $dev as $line")
          .map(line ⇒ tracks += ((0, dev, begin, line)))
          .map(action ⇒ action.flatMap(_ ⇒ updateOffsetQuery(Journaler.Id, Tags.Tracks, offset)))
          .mapAsync(1)(action ⇒ db.run(action.transactionally))
          .runWith(Sink.actorRef(self, TrackPersisted(id)))

      case _ ⇒
    }
  }

  def receive: Receive = {
    case Success(_) ⇒ log.debug("read-side journal success")
    case Failure(ex) ⇒ log.error(ex, "read-side journal failure")
    case TrackPersisted(id) ⇒ log.debug("persisted track {}", id)
    case MovePersisted(dev, pt) ⇒ log.debug("persisted move {} to {}", dev, pt)
  }
}
