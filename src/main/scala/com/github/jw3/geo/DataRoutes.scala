package com.github.jw3.geo

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.github.jw3.geo.GeoConcepts.TrackTable
import com.github.jw3.geo.PgDriver.api._

import scala.util.{Failure, Success, Try}

object DataRoutes {}

trait DataRoutes {
  import akka.http.scaladsl.server.Directives._

  def dataRoutes(db: Database): Route =
    extractLog { logger ⇒
      extractExecutionContext { implicit ec ⇒
        pathPrefix("api") {
          path("track" / Segment) { id ⇒
            get {
              import geotrellis.vector.io.json.Implicits._

              Try(id.toInt) match {
                case Success(v) ⇒
                  val q = TrackTable.tracks.filter(_.id === v).take(1)
                  complete(db.run(q.result).map(_.head._4.toGeoJson()))
                case Failure(_) ⇒
                  complete(StatusCodes.BadRequest)
              }
            }
          }
        }
      }
    }
}
