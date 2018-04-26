package com.github.jw3.geo

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import akka.persistence.RecoveryCompleted
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.github.jw3.geo.Api.{Commands, Responses}
import com.github.jw3.geo.DevicePersistenceSpec._
import com.github.jw3.geo.test.random
import geotrellis.vector.Point
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class DevicePersistenceSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with ImplicitSender {
  implicit val timeout: Timeout = Timeout(5 seconds)

  "persisted devices" should {
    "restore when empty" in {
      val pid = random()

      {
        val mgr = mockQuietly(pid)
        kill(mgr)
      }
      {
        mock(pid)
        expectMsg(RecoveryCompleted)
      }
    }

    "replay position events" in {
      val pid = random()
      val expected = Point(1, 1)

      {
        val mgr = mockQuietly(pid)
        mgr ! Commands.MoveDevice(pid, Point(0, 0))
        mgr ! Commands.MoveDevice(pid, expected)
        Thread.sleep(500)

        kill(mgr)
      }
      {
        val mgr = mock(pid)
        expectMsg(RecoveryCompleted)

        mgr ! Commands.GetDevicePosition(pid)
        expectMsg(Responses.DevicePosition(pid, expected))
      }
    }
  }

  def kill(ref: ActorRef): Terminated = {
    watch(ref)
    ref ! PoisonPill
    expectTerminated(ref)
  }
}

object DevicePersistenceSpec {
  def mockQuietly(pid: String)(implicit system: ActorSystem): ActorRef =
    mock(pid)(system, ActorRef.noSender)

  def mock(pid: String)(implicit system: ActorSystem, listener: ActorRef): ActorRef = {
    system.actorOf(Props(new Device(pid) {
      override def receiveRecover: Receive = {
        case RecoveryCompleted ⇒
          Option(listener).foreach(_.tell(RecoveryCompleted, ActorRef.noSender))
          super.receiveRecover.apply(RecoveryCompleted)
        case m ⇒ super.receiveRecover(m)
      }
    }))
  }
}
