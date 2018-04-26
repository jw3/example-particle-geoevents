package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, NoOffset, PersistenceQuery}
import akka.stream.ActorMaterializer
import com.github.jw3.geo.Api.Events.PositionUpdate
import com.github.jw3.geo.GeoConcepts.EventTable.events
import com.github.jw3.geo.PgDriver.api._

import scala.util.{Failure, Success}

object Journaler {
  def props(db: Database)(implicit mat: ActorMaterializer) = Props(new Journaler(db))
}

class Journaler(db: Database)(implicit mat: ActorMaterializer) extends Actor with ActorLogging {
  import context.dispatcher

  val readJournal: JdbcReadJournal =
    PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  readJournal.currentEventsByTag("event", NoOffset).runForeach {
    case EventEnvelope(_, _, _, PositionUpdate(dev, pos)) ⇒
      val action = events += ((0, dev, pos))
      db.run(action).onComplete(context.self ! _)
    case _ ⇒
  }

  def receive: Receive = {
    case Success(_) ⇒ log.info("success")
    case Failure(_) ⇒ log.info("fail")
  }
}
