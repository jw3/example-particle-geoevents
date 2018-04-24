package com.github.jw3.geo

import com.github.tminglei.slickpg.ExPostgresProfile
import geotrellis.slick.PostGisSupport
import geotrellis.vector.Point

import scala.util.Try

trait PgDriver extends ExPostgresProfile with PostGisSupport {
  override val api = GeoEventApi
  object GeoEventApi extends API with PostGISImplicits
}
object PgDriver extends PgDriver

object GeoConcepts {
  import com.github.jw3.geo.PgDriver.api._

  class EventTable(tag: Tag) extends Table[(Int, String, Point)](tag, "events") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def device = column[String]("device")
    def geometry = column[Point]("geometry")
    def * = (id, device, geometry)
  }
  object EventTable {
    val events = TableQuery[EventTable]
  }
}

trait GeoDatabase {
  import com.github.jw3.geo.PgDriver.api._

  private var db: Option[Database] = None

  def initdb(): Option[Database] = {
    if (db.isEmpty) db = {
      import com.github.jw3.geo.PgDriver.api._
      Try(Database.forConfig("events")).toOption
    }
    db
  }
}
