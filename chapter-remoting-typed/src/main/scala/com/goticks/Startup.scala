package com.goticks

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

trait Startup extends RequestTimeout {

  private def startHttpServer(api: Route, host: String, port: Int)(implicit system: ActorSystem): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher

    val bindingFuture = Http().bindAndHandle(api, host, port)

    val log = Logging(system.eventStream, "go-ticks")

    bindingFuture
      .map { serverBinding => log.info(s"RestApi bound to ${serverBinding.localAddress}") }
      .onComplete {
        case Failure(cause) =>
          log.error(cause, "Failed to bind to {}:{}", host, port)
          system.terminate()
        case Success(_) =>
      }
  }

  def startup(api: Route)(implicit system: ActorSystem): Unit = {
    val host = system.settings.config.getString("http.host")
    val port = system.settings.config.getInt("http.port")
    startHttpServer(api, host, port)
  }

}
