package com.goticks

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }
import akka.util.Timeout
import com.goticks.actor.BoxOffice
import com.typesafe.config.ConfigFactory

object BackendMain extends App with RequestTimeout {
  val config                           = ConfigFactory.load("backend")
  implicit val requestTimeout: Timeout = configuredRequestTimeout(config)
  ActorSystem[Nothing](behavior, "backend", config)

  def behavior: Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
    val boxOfficeRef = ctx.spawn(BoxOffice(), BoxOffice.name)
    ctx.system.receptionist ! Receptionist.Register(BoxOffice.serviceKey, boxOfficeRef)
    Behaviors.empty
  }
}
