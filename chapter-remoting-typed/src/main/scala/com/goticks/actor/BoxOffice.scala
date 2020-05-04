package com.goticks.actor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import com.goticks.domain.model._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object BoxOffice {
  def name: String = "boxOffice"

  sealed trait Command

  final case class CreateEvent(eventName: EventName, eventTickets: EventTickets, replyTo: ActorRef[EventCreated])
      extends Command
  sealed trait EventCreated
  final case class EventCreatedSuccessful(event: Event) extends EventCreated
  final case object EventExists                         extends EventCreated

  final case class GetEvent(eventName: EventName, replyTo: ActorRef[EventGot])      extends Command
  final case class WrappedGetEvent(eventGot: EventGot, replyTo: ActorRef[EventGot]) extends Command
  final case class EventGot(event: Option[Event])

  final case class GetEvents(replyTo: ActorRef[EventsGot])                              extends Command
  final case class WrappedGetEvents(eventsGot: EventsGot, replyTo: ActorRef[EventsGot]) extends Command
  sealed trait EventsGot
  final case class EventsGotSuccessful(events: Events)    extends EventsGot
  final case class EventsGotFailure(exception: Throwable) extends EventsGot

  final case class GetTickets(eventName: EventName, eventTickets: EventTickets, replyTo: ActorRef[TicketsGot])
      extends Command
  final case class WrappedGetTickets(ticketsGot: TicketsGotSuccessful, replyTo: ActorRef[TicketsGot]) extends Command
  sealed trait TicketsGot
  final case class TicketsGotSuccessful(tickets: Tickets) extends TicketsGot
  final case object SoldOut                               extends TicketsGot

  final case class CancelEvent(eventName: EventName, replyTo: ActorRef[EventCanceled])                extends Command
  final case class WrappedCancelEvent(eventCanceled: EventCanceled, replyTo: ActorRef[EventCanceled]) extends Command
  final case class EventCanceled(event: Option[Event])

  trait TicketSellerRefCreator {

    def createTicketSellerRef(
        ctx: ActorContext[Command],
        createEvent: CreateEvent,
        childName: String
    ): ActorRef[TicketSeller.Command]
  }

  object TicketSellerRefCreatorImpl extends TicketSellerRefCreator {

    override def createTicketSellerRef(
        ctx: ActorContext[Command],
        createEvent: CreateEvent,
        childName: String
    ): ActorRef[TicketSeller.Command] =
      ctx.spawn(TicketSeller(createEvent.eventName), childName)
  }

  def apply()(
      implicit timeout: Timeout,
      creator: TicketSellerRefCreator = TicketSellerRefCreatorImpl
  ): Behavior[Command] = Behaviors.setup { ctx =>
    implicit val system: ActorSystem[_] = ctx.system
    import ctx.executionContext

    Behaviors.receiveMessage {
      case createEvent @ CreateEvent(eventName, _, replyTo) =>
        def create(ctx: ActorContext[Command], createEvent: CreateEvent, childName: String): Unit = {
          val ticketSellerRef         = creator.createTicketSellerRef(ctx, createEvent, childName)
          val newTickets: Seq[Ticket] = createEvent.eventTickets.createNewTickets
          ticketSellerRef ! TicketSeller.Add(newTickets)
          createEvent.replyTo ! EventCreatedSuccessful(Event(createEvent.eventName, createEvent.eventTickets))
        }

        val name = TicketSeller.name(eventName)
        ctx.child(name).fold(create(ctx, createEvent, name))(_ => replyTo ! EventExists)
        Behaviors.same

      case GetTickets(eventName, eventTickets, replyTo) =>
        def notFound(): Unit = replyTo ! SoldOut
        def buy(child: ActorRef[TicketSeller.Command]): Unit = {
          val reply: ActorRef[TicketSeller.TicketsGot] =
            ctx.messageAdapter(ticketSellerTicketsGot =>
              WrappedGetTickets(TicketsGotSuccessful(ticketSellerTicketsGot.tickets), replyTo)
            )
          child ! TicketSeller.Buy(eventTickets, reply)
        }
        val name = TicketSeller.name(eventName)
        ctx.child(name).fold(notFound())(a => buy(a.asInstanceOf[ActorRef[TicketSeller.Command]]))
        Behaviors.same

      case WrappedGetTickets(ticketsGot, replyTo) =>
        replyTo ! ticketsGot
        Behaviors.same

      case GetEvent(eventName, replyTo) =>
        def notFound(): Unit = replyTo ! EventGot(None)
        def send(child: ActorRef[TicketSeller.Command]): Unit = {
          val mapped: ActorRef[TicketSeller.EventGot] =
            ctx.messageAdapter(ticketSellerEventGot => WrappedGetEvent(EventGot(ticketSellerEventGot.maybe), replyTo))
          child ! TicketSeller.GetEvent(mapped)
        }

        val name = TicketSeller.name(eventName)
        ctx.child(name).fold(notFound())(a => send(a.asInstanceOf[ActorRef[TicketSeller.Command]]))
        Behaviors.same

      case WrappedGetEvent(eventGot, replyTo) =>
        replyTo ! eventGot
        Behaviors.same

      case GetEvents(replyTo) =>
        def getEvents: Seq[Future[EventGot]] =
          ctx.children.map { child =>
            ctx.self.ask[EventGot](reply => GetEvent(TicketSeller.takeEventName(child.path.name), reply))
          }.toSeq
        def convert(f: Future[Seq[EventGot]]): Future[EventsGot] =
          f.map(_.flatMap(_.event)).map(l => EventsGotSuccessful(Events(l)))

        ctx.pipeToSelf(convert(Future.sequence(getEvents))) {
          case Success(eventsGot) => WrappedGetEvents(eventsGot, replyTo)
          case Failure(exception) => WrappedGetEvents(EventsGotFailure(exception), replyTo)
        }
        Behaviors.same

      case WrappedGetEvents(eventsGot, replyTo) =>
        replyTo ! eventsGot
        Behaviors.same

      case CancelEvent(eventName, replyTo) =>
        def notFound(): Unit = replyTo ! EventCanceled(None)
        def cancelEvent(child: ActorRef[TicketSeller.Command]): Unit = {
          val mapped: ActorRef[TicketSeller.Canceled] = ctx.messageAdapter(ticketSellerCanceled =>
            WrappedCancelEvent(EventCanceled(ticketSellerCanceled.maybe), replyTo)
          )
          child ! TicketSeller.Cancel(mapped)
        }

        val name = TicketSeller.name(eventName)
        ctx.child(name).fold(notFound())(a => cancelEvent(a.asInstanceOf[ActorRef[TicketSeller.Command]]))
        Behaviors.same

      case WrappedCancelEvent(eventCanceled, replyTo) =>
        replyTo ! eventCanceled
        Behaviors.same
    }
  }

}
