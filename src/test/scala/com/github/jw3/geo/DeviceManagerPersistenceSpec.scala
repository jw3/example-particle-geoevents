package com.github.jw3.geo

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{Matchers, WordSpecLike}
import test._
import DeviceManagerPersistenceSpec._
import com.github.jw3.geo.Api.{Commands, Events}

import scala.concurrent.duration.DurationInt

class DeviceManagerPersistenceSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with ImplicitSender {
  implicit val timeout: Timeout = Timeout(5 seconds)

  "persisted devices" should {
    "restore when empty" in {
      val pid = random()

      {
        val mgr = mockService(pid)
        kill(mgr)
      }
      {
        val mgr = mockService(pid)
        mgr ! ChildCount
        expectMsg(0)
      }
    }

    "restore device" in {
      val pid = random()
      val dev = random()

      {
        val mgr = mockService(pid)
        mgr ! Commands.AddDevice(dev)
        expectMsg(Events.DeviceAdded(dev))
        kill(mgr)
      }
      {
        val mgr = mockService(pid)
        mgr ! DeviceManager.QueryDeviceStatus(dev)
        expectMsg(DeviceManager.DeviceOnline)
      }
    }
  }

  def kill(ref: ActorRef): Terminated = {
    watch(ref)
    ref ! PoisonPill
    expectTerminated(ref)
  }
}

object DeviceManagerPersistenceSpec {
  case object ChildCount

  def mockService(pid: String)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new DeviceManager {
      override def persistenceId: String = pid

      override def receiveCommand: Receive = super.receiveCommand orElse {
        case ChildCount ⇒ sender.tell(context.children.size, self)
      }
    }))
  }
}
