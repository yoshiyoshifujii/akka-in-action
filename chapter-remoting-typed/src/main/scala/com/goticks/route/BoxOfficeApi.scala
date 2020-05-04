package com.goticks.route

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.util.Timeout
import com.goticks.domain.model.{EventName, EventTickets}

import scala.concurrent.{ExecutionContext, Future}

trait BoxOfficeApi {
  import com.goticks.actor.BoxOffice._

  def log: LoggingAdapter
  def createBoxOffice(): ActorRef[Command]
  implicit val system: ActorSystem[_]// = ctx.system
  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val boxOffice: ActorRef[Command] = createBoxOffice()

  def createEvent(eventName: EventName, nrOfTickets: EventTickets): Future[EventCreated] = {
    log.info(s"Received new event $eventName, sending to $boxOffice")
    boxOffice.ask[EventCreated](reply => CreateEvent(eventName, nrOfTickets, reply))
  }

  def getEvents: Future[EventsGot] =
    boxOffice.ask[EventsGot](reply => GetEvents(reply))

  def getEvent(eventName: EventName): Future[EventGot] =
    boxOffice.ask[EventGot](reply => GetEvent(eventName, reply))

  def cancelEvent(eventName: EventName): Future[EventCanceled] =
    boxOffice.ask[EventCanceled](reply => CancelEvent(eventName, reply))

  def requestTickets(eventName: EventName, eventTickets: EventTickets): Future[TicketsGot] =
    boxOffice.ask[TicketsGot](reply => GetTickets(eventName, eventTickets, reply))

}
