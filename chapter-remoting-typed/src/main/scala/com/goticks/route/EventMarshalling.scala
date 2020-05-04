package com.goticks.route

import com.goticks.domain.model._
import spray.json._

case class EventDescription(tickets: Int) {
  require(tickets > 0)

  def eventTickets: EventTickets = EventTickets(tickets)
}

case class TicketRequest(tickets: Int) {
  require(tickets > 0)

  def eventTickets: EventTickets = EventTickets(tickets)
}

case class Error(message: String)

trait EventMarshalling extends DefaultJsonProtocol {

  implicit val EventDescriptionFormat = jsonFormat1(EventDescription)
  implicit val EventNameFormat        = jsonFormat1(EventName)
  implicit val EventTicketsFormat     = jsonFormat1(EventTickets)
  implicit val EventFormat            = jsonFormat2(Event)
  implicit val EventsFormat           = jsonFormat1(Events)
  implicit val TicketRequestFormat    = jsonFormat1(TicketRequest)
  implicit val TicketIdFormat         = jsonFormat1(TicketId)
  implicit val TicketFormat           = jsonFormat1(Ticket)
  implicit val TicketsFormat          = jsonFormat2(Tickets)
  implicit val ErrorFormat            = jsonFormat1(Error)
}
