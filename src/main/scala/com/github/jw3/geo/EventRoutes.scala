package com.github.jw3.geo

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.jw3.geo.Api.Events.PositionUpdate
import com.github.jw3.geo.DeviceReadSide.RequestStreamTo

object EventRoutes {}

trait EventRoutes {
  import akka.http.scaladsl.server.Directives._

  def eventRoutes(deviceReadSide: ActorRef): Route =
    pathPrefix("api") {
      path("watch" / "device") {
        get {
          extractUpgradeToWebSocket { upgrade ⇒
            complete {
              val source = Source
                .actorRef[PositionUpdate](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
                .map(pu ⇒ TextMessage(pu.asTxtMsg()))
                .mapMaterializedValue(ref ⇒ deviceReadSide ! RequestStreamTo(ref))
              upgrade.handleMessages(Flow.fromSinkAndSource(Sink.ignore, source))
            }
          }
        }
      }
    }
}
