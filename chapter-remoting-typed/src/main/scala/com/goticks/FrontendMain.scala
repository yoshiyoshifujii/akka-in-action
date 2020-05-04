package com.goticks

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ typed, ActorSystem, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.util.Timeout
import com.goticks.actor.{ BoxOffice, RemoteLookupProxy }
import com.goticks.route.RestApi
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

object FrontendMain extends App with Startup {

  val config = ConfigFactory.load("frontend")

  implicit val actorSystem: ActorSystem = ActorSystem("frontend", config)

  val api: RestApi = new RestApi {
    override val log: LoggingAdapter                         = Logging(actorSystem.eventStream, "frontend")
    override implicit val system: typed.ActorSystem[_]       = actorSystem.toTyped
    override implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    override implicit val requestTimeout: Timeout            = configuredRequestTimeout(config)

    private def createPath: String = {
      val config     = ConfigFactory.load("frontend").getConfig("backend")
      val host       = config.getString("host")
      val port       = config.getInt("port")
      val protocol   = config.getString("protocol")
      val systemName = config.getString("system")
      val actorName  = config.getString("actor")
      s"$protocol://$systemName@$host:$port/$actorName"
    }

    override def createBoxOffice(): ActorRef[BoxOffice.Command] = {
      val path = createPath
      actorSystem.actorOf(Props(new RemoteLookupProxy(path)), "lookupBoxOffice")
    }
  }

  startup(api.routes)

}
