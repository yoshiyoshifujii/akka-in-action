package com.goticks

import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._

trait RequestTimeout {

  def configuredRequestTimeout(config: Config): Timeout =
    config.getDuration("akka.http.server.request-timeout").toMillis.milli

}
