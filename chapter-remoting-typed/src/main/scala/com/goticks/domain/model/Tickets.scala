package com.goticks.domain.model

case class Tickets(eventName: EventName, values: Seq[Ticket] = Seq.empty)
