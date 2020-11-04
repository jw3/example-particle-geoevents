package com.github.jw3.geo

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.persistence.RecoveryCompleted
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.github.jw3.geo.Api.{Commands, Events}
import com.github.jw3.geo.DeviceManagerPersistenceSpec._
import com.github.jw3.geo.test._
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class DeviceManagerPersistenceSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with ImplicitSender {
  implicit val timeout: Timeout = Timeout(5 seconds)

  "device manager" should {
    "restore when empty" in {
      val pid = random()

      {
        val mgr = mockServiceQuietly(pid)
        kill(mgr)
      }
      {
        val mgr = mockService(pid)
        expectMsg(RecoveryCompleted)

        mgr ! ChildCount
        expectMsg(0)
      }
    }

    "restore device" in {
      val pid = random()
      val dev = random()

      {
        val mgr = mockServiceQuietly(pid)
        mgr ! Commands.AddDevice(dev, None)
        expectMsg(Events.DeviceAdded(dev))
        kill(mgr)
      }
      {
        val mgr = mockService(pid)
        expectMsg(RecoveryCompleted)

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

  def mockServiceQuietly(pid: String)(implicit system: ActorSystem): ActorRef =
    mockService(pid)(system, ActorRef.noSender)

  def mockService(pid: String)(implicit system: ActorSystem, listener: ActorRef): ActorRef = {
    system.actorOf(Props(new DeviceManager {
      override val persistenceId: String = pid

      override def receiveRecover: Receive = {
        case RecoveryCompleted ⇒
          Option(listener).foreach(_.tell(RecoveryCompleted, ActorRef.noSender))
          super.receiveRecover.apply(RecoveryCompleted)
        case m ⇒ super.receiveRecover(m)
      }

      override def receiveCommand: Receive = super.receiveCommand orElse {
        case ChildCount ⇒ sender.tell(context.children.size, self)
      }
    }))
  }
}
