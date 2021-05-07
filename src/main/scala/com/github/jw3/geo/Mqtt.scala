package com.github.jw3.geo

import akka.Done
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.Source
import com.github.jw3.geo.Api.Commands
import com.github.jw3.geo.DeviceRoutes.{MalformedEvent, UnsupportedEvent}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import geotrellis.vector.Point
import net.ceedubs.ficus.Ficus._
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import spray.json._

import scala.concurrent.Future
import scala.util.Try

object Mqtt extends LazyLogging {
  private val Unused = "__unused__"
  val tobeFunc = "move"
  val asisFunc = "move"

  def start(config: Config): Source[Api.Command, Future[Done]] = {
    val mqttConf = config.as[Config]("mqtt")
    val mqttClientId = config.as[String]("mqtt.clientid")
    Mqtt
      .sub(mqttClientId, mqttConf)
      .map {
        case DeviceRoutes.ReadyEvent(id, version) ⇒ Commands.AddDevice(id, Some(version))
        case DeviceRoutes.MoveEvent(id, x, y) ⇒ Commands.MoveDevice(id, Point(x, y))
        case DeviceRoutes.DisconnectEvent(id) ⇒ Commands.Disconnected(id)
        case _ ⇒ Commands.Nop
      }
      .filter {
        case Commands.Nop ⇒ false
        case _ ⇒ true
      }
  }

  private def settings(config: Config): MqttConnectionSettings = {
    val brokerHost = config.as[String]("host")
    val brokerPort = config.getAs[Int]("port").getOrElse(1883)
    val mqttUri = s"tcp://$brokerHost:$brokerPort"
    val settings = MqttConnectionSettings(mqttUri, Unused, new MemoryPersistence)

    (config.getAs[String]("user"), config.getAs[String]("pass")) match {
      case (Some(user), Some(pass)) ⇒ settings.withAuth(user, pass)
      case _ ⇒ settings
    }
  }

  private def sub(deviceId: String, config: Config): Source[DeviceRoutes.IncomingEvent, Future[Done]] = {
    val prefix: String = config.as[String]("events.prefix")
    object Topics {
      val ready = s"$prefix/${config.as[String]("events.ready")}"
      val moved = s"$prefix/${config.as[String]("events.moved")}"
      val disco = s"$prefix/${config.as[String]("events.disco")}"
    }
    val cs = settings(config)

    MqttSource
      .atMostOnce(cs.withClientId(deviceId), MqttSubscriptions(s"$prefix/#", MqttQoS.atLeastOnce), bufferSize = 1000)
      .map { m =>
        Try(m.payload.utf8String.parseJson)
          .map { json ⇒
            m.topic match {
              case Topics.ready ⇒ json.convertTo[DeviceRoutes.ReadyEvent]
              case Topics.moved ⇒ json.convertTo[DeviceRoutes.MoveEvent]
              case Topics.disco ⇒ json.convertTo[DeviceRoutes.DisconnectEvent]
              case _ ⇒ UnsupportedEvent(m.payload.utf8String)
            }
          }
          .getOrElse(MalformedEvent(m.payload.utf8String))
      }
      .wireTap { e ⇒
        e match {
          case MalformedEvent(payload) ⇒ logger.warn("malformed mqtt event: {}", payload)
          case _ ⇒
        }
      }
  }
}
