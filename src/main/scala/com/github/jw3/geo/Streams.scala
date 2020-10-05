package com.github.jw3.geo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, NoOffset, Offset, PersistenceQuery}
import akka.stream.scaladsl.Source
import com.github.jw3.geo.Api.Tags

object Streams {
  private def stream(tag: String, offset: Offset)(implicit sys: ActorSystem): Source[EventEnvelope, NotUsed] =
    PersistenceQuery(sys).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier).eventsByTag(tag, offset)

  def movement(offset: Offset = NoOffset)(implicit sys: ActorSystem): Source[EventEnvelope, NotUsed] =
    stream(Tags.Movement, offset)

  def tracking(offset: Offset = NoOffset)(implicit sys: ActorSystem): Source[EventEnvelope, NotUsed] =
    stream(Tags.Tracks, offset)

  def fencing(offset: Offset = NoOffset)(implicit sys: ActorSystem): Source[EventEnvelope, NotUsed] =
    stream(Tags.Fencing, offset)
}
