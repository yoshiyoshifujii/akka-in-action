package com.goticks

import akka.actor.typed.ActorRef
import akka.actor.{ typed, ActorSystem }
import akka.actor.typed.scaladsl.adapter._
import akka.event.{ Logging, LoggingAdapter }
import akka.util.Timeout
import com.goticks.actor.BoxOffice
import com.goticks.route.RestApi
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object SingleNodeMain extends App with Startup {
  val config                            = ConfigFactory.load("singlenode")
  implicit val actorSystem: ActorSystem = ActorSystem("singlenode", config)

  val api: RestApi = new RestApi {
    override val log: LoggingAdapter                            = Logging(actorSystem.eventStream, "go-ticks")
    override implicit val system: typed.ActorSystem[_]          = actorSystem.toTyped
    override implicit val executionContext: ExecutionContext    = actorSystem.dispatcher
    override implicit val requestTimeout: Timeout               = configuredRequestTimeout(config)
    override def createBoxOffice(): ActorRef[BoxOffice.Command] = actorSystem.spawn(BoxOffice(), BoxOffice.name)
  }

  startup(api.routes)
}
