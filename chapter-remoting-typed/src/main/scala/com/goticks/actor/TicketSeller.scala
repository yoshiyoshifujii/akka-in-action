package com.goticks.actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.goticks.domain.model._

object TicketSeller {

  final case class TicketsGot(tickets: Tickets)
  final case class EventGot(maybe: Option[Event])
  final case class Canceled(maybe: Option[Event])

  sealed trait Command
  final case class Add(tickets: Seq[Ticket])                                      extends Command
  final case class Buy(eventTickets: EventTickets, replyTo: ActorRef[TicketsGot]) extends Command
  final case class GetEvent(replyTo: ActorRef[EventGot])                          extends Command
  final case class Cancel(replyTo: ActorRef[Canceled])                            extends Command

  private val PrefixName: String             = "ticketSeller"
  def name(eventName: EventName): String     = s"$PrefixName-${eventName.value}"
  def takeEventName(name: String): EventName = EventName(name.replaceFirst(s"^$PrefixName-", ""))

  def apply(eventName: EventName): Behavior[Command] = Behaviors.setup { _ =>
    var tickets = Seq.empty[Ticket]

    Behaviors.receiveMessage {
      case Add(newTickets) =>
        tickets = tickets ++ newTickets
        Behaviors.same
      case Buy(nrOfTickets, replyTo) =>
        // FIXME domain logic
        val entries = tickets.take(nrOfTickets.value)
        if (entries.size >= nrOfTickets.value) {
          replyTo ! TicketsGot(Tickets(eventName, entries))
          tickets = tickets.drop(nrOfTickets.value)
        } else {
          replyTo ! TicketsGot(Tickets(eventName))
        }
        Behaviors.same
      case GetEvent(replyTo) =>
        replyTo ! EventGot(Some(Event(eventName, EventTickets(tickets.size))))
        Behaviors.same
      case Cancel(replyTo) =>
        replyTo ! Canceled(Some(Event(eventName, EventTickets(tickets.size))))
        Behaviors.stopped
    }
  }

}
