package com.goticks.domain.model

case class EventTickets(value: Int) {

  def createNewTickets: Seq[Ticket] = (1 to value).map(i => Ticket(TicketId(i)))
}
