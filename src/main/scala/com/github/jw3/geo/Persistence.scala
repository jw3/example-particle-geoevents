package com.github.jw3.geo

import java.util.UUID

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{NoOffset, Offset, TimeBasedUUID, Sequence ⇒ AkkaSequence}
import com.github.jw3.geo.Api.Events.{PositionUpdate, TrackingEvent}
import com.github.jw3.geo.Api.Tags
import com.github.tminglei.slickpg.ExPostgresProfile
import com.typesafe.config.Config
import geotrellis.slick.PostGisSupport
import geotrellis.vector.{Line, Point}

import scala.util.Try

trait PgDriver extends ExPostgresProfile with PostGisSupport {
  final override val api = GeoEventApi
  object GeoEventApi extends API with PostGISImplicits
}
object PgDriver extends PgDriver

object GeoConcepts {
  import com.github.jw3.geo.PgDriver.api._

  class EventTable(tag: Tag) extends Table[(Int, String, Point)](tag, "positions") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def device = column[String]("device")
    def geometry = column[Point]("geometry")
    def * = (id, device, geometry)
  }
  object EventTable {
    val events = TableQuery[EventTable]
  }

  class TrackTable(tag: Tag) extends Table[(Int, String, Long, Line)](tag, "tracks") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def device = column[String]("device")
    def startseq = column[Long]("startseq")
    def geometry = column[Line]("geometry")
    def * = (id, device, startseq, geometry)
  }
  object TrackTable {
    val tracks = TableQuery[TrackTable]
  }
}

trait GeoDatabase {
  import com.github.jw3.geo.PgDriver.api._

  def initdb(config: Config): Try[Database] = {
    import com.github.jw3.geo.PgDriver.api._
    Try(Database.forConfig("slick.db", config))
  }
}

class TaggingEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  def withTag(event: Any, tag: String) = Tagged(event, Set(tag))

  override def toJournal(event: Any): Any = event match {
    case _: PositionUpdate ⇒
      withTag(event, Tags.Movement)
    case _: TrackingEvent ⇒
      withTag(event, Tags.Tracks)
    case _ ⇒ event
  }
}

/*
 * Copyright (C) 2016-2018 Lightbend Inc. <https://www.lightbend.com>
 * https://github.com/lagom/lagom/blob/master/persistence-jdbc/core/src/main/scala/com/lightbend/lagom/internal/persistence/jdbc/SlickOffsetStore.scala
 */
object OffsetStore {
  import com.github.jw3.geo.PgDriver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  def updateOffsetQuery(id: String, tag: String, offset: Offset) = {
    offsets.insertOrUpdate(queryToOffsetRow(id, tag, offset))
  }

  case class OffsetRow(id: String, tag: String, sequenceOffset: Option[Long], timeUuidOffset: Option[String])

  private[geo] class OffsetTable(_tag: Tag) extends Table[OffsetRow](_tag, "read_side_offsets") {
    val id = column[String]("read_side_id", O.Length(255, varying = true), O.PrimaryKey)
    val tag = column[String]("tag", O.Length(255, varying = true), O.PrimaryKey)
    val sequenceOffset = column[Option[Long]]("sequence_offset")
    val timeUuidOffset = column[Option[String]]("time_uuid_offset", O.Length(36, varying = false))
    val pk = primaryKey("read_side_offsets_pk", (id, tag))
    def * = (id, tag, sequenceOffset, timeUuidOffset) <> (OffsetRow.tupled, OffsetRow.unapply)
  }

  private[geo] val offsets = TableQuery[OffsetTable]

  private def queryToOffsetRow(id: String, tag: String, offset: Offset): OffsetRow = {
    offset match {
      case AkkaSequence(value)  => OffsetRow(id, tag, Some(value), None)
      case TimeBasedUUID(value) => OffsetRow(id, tag, None, Some(value.toString))
      case NoOffset             => OffsetRow(id, tag, None, None)
    }
  }

  private[geo] def getOffsetQuery(id: String, tag: String): DBIOAction[Offset, NoStream, Effect.Read] = {
    (for {
      offset <- offsets if offset.id === id && offset.tag === tag
    } yield {
      offset
    }).result.headOption.map(offsetRowToOffset)
  }

  private def offsetRowToOffset(row: Option[OffsetRow]): Offset = {
    row
      .flatMap(
        row =>
          row.sequenceOffset
            .map(AkkaSequence)
            .orElse(
              row.timeUuidOffset
                .flatMap(uuid => Try(UUID.fromString(uuid)).toOption)
                .filter(_.version == 1)
                .map(TimeBasedUUID)
          )
      )
      .getOrElse(NoOffset)
  }
}
