package com.github.jw3.geo

import akka.actor.{Actor, ActorLogging, Props}

object Device {
  def props() = Props(new Device)
}

class Device extends Actor with ActorLogging {
  def receive: Receive = {
    case _ ⇒
  }
}
