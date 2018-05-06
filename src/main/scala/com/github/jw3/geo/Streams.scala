package com.github.jw3.geo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, NoOffset, Offset, PersistenceQuery}
import akka.stream.scaladsl.Source
import com.github.jw3.geo.Api.Tags

object Streams {
  def movement(offset: Offset = NoOffset)(implicit sys: ActorSystem): Source[EventEnvelope, NotUsed] = {
    PersistenceQuery(sys).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier).eventsByTag(Tags.Movement, offset)
  }
}
