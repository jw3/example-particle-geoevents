package com.github.jw3.geo

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.github.jw3.geo.Api.{Commands, Events, Responses}
import com.github.jw3.geo.Device.Track
import geotrellis.vector.Point
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object Device {
  def props(id: String) = Props(new Device(id))

  case class Snapshot(position: Option[Point], track: Option[Track])
  case class Track(id: String, seqNr: Long, startPt: Point)
  object Responses extends DefaultJsonProtocol with geotrellis.vector.io.json.Implicits {
    implicit val format: RootJsonFormat[Track] = jsonFormat3(Track.apply)
  }
}

class Device(id: String) extends PersistentActor with ActorLogging {
  val persistenceId: String = id
  var position: Option[Point] = None
  var tracking: Option[Track] = None

  def receiveRecover: Receive = {
    case RecoveryCompleted ⇒
      log.debug(s"device {} created", id)

    case SnapshotOffer(_, ss: Device.Snapshot) ⇒
      log.debug("snap device {} to {}", id, ss.position)
      position = ss.position
      tracking = ss.track

    case Events.PositionUpdate(_, pos) ⇒
      log.debug("replayed device move {}", pos)
      position = Some(pos)

    case Events.TrackStarted(tid, _, seq, pt) ⇒
      log.debug("replayed device track starting")
      tracking = Some(Track(tid, seq, pt))

    case _: Events.TrackCompleted ⇒
      log.debug("replayed device track ending")
      tracking = None
  }

  def receiveCommand: Receive = {
    //
    // commands
    //
    case Commands.MoveDevice(_, g) ⇒
      persist(Events.PositionUpdate(id, g)) { e ⇒
        //
      }

    case Commands.StartTracking(`id`) if tracking.isEmpty ⇒
      position.foreach { pt ⇒
        val seqNr = lastSequenceNr + 1
        val trackId = UUID.randomUUID.toString.take(8)
        persist(Events.TrackStarted(trackId, id, seqNr, pt)) { e ⇒
          tracking = Some(Track(e.id, e.beginSeqNr, pt))
        }
      }

    case Commands.StopTracking(`id`) ⇒
      for {
        end ← position
        t ← tracking
      } {
        persist(Events.TrackCompleted(t.id, id, t.seqNr, lastSequenceNr, t.startPt, end)) { _ ⇒
          tracking = None
        }
      }

    //
    // events
    //
    case e @ Events.PositionUpdate(`id`, _) ⇒
    //
    // read-only commands
    //
    case Commands.GetDevicePosition(`id`) ⇒
      position match {
        case None ⇒ sender ! Responses.UnknownDevicePosition(id)
        case Some(p) ⇒ sender ! Responses.DevicePosition(id, p)
      }
  }
}
