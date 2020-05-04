package com.goticks

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import com.goticks.actor.BoxOffice
import com.typesafe.config.ConfigFactory

object BackendMain extends App with RequestTimeout {
  val config = ConfigFactory.load("backend")
  implicit val requestTimeout: Timeout = configuredRequestTimeout(config)
  ActorSystem[BoxOffice.Command](BoxOffice(), BoxOffice.name)
}
