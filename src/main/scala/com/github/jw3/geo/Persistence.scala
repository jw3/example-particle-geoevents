package com.github.jw3.geo

import akka.persistence.journal.{Tagged, WriteEventAdapter}
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
