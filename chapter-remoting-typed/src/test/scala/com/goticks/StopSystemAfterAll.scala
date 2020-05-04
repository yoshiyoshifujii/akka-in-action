package com.goticks

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait StopSystemAfterAll extends BeforeAndAfterAll {
  self: Suite =>

  val testKit: ActorTestKit = ActorTestKit()

  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }
}
