package com.github.jw3.geo

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink}
import com.github.jw3.geo.Api.{Commands, FenceData}
import com.github.jw3.geo.Api.Events.{EnteredFence, ExitedFence, FenceContainment, FencingChange}
import geotrellis.vector.io.json.GeoJsonSupport._
import geotrellis.vector.io.json.JsonFeatureCollection

trait FenceRoutes {
  import akka.http.scaladsl.server.Directives._

  def fenceRoutes(fencer: ActorRef): Route =
    pathPrefix("api") {
      extractActorSystem { implicit system ⇒
        pathPrefix("fence") {
          post {
            entity(as[JsonFeatureCollection]) { fc ⇒
              val fs = fc.getAllPolygonFeatures[FenceData]()
                fs.foreach(f ⇒ fencer ! Commands.CreateFence(f.data.name, f.geom))
              complete(StatusCodes.Accepted)
            }
          } ~
            get {
              path("watch") {
                extractUpgradeToWebSocket { upgrade ⇒
                  complete {
                    val source = Streams
                      .fencing()
                      .map(_.event)
                      .filter {
                        case _: FenceContainment ⇒ true
                        case _ ⇒ false
                      }
                      .map {
                        case EnteredFence(id, _, dev) ⇒ FencingChange.enter(id, dev)
                        case ExitedFence(id, _, dev) ⇒ FencingChange.exit(id, dev)
                      }
                      .map(_.toJson.compactPrint)
                      .map(TextMessage(_))
                    upgrade.handleMessages(Flow.fromSinkAndSource(Sink.ignore, source))
                  }
                }
              }
            }
        }
      }
    }
}
