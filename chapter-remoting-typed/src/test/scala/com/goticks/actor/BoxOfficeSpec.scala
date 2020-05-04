package com.goticks.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import com.goticks.StopSystemAfterAll
import com.goticks.domain.model._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BoxOfficeSpec extends AnyWordSpec with Matchers with StopSystemAfterAll {

  implicit val timeout: Timeout = testKit.timeout

  "The BoxOffice" must {

    "Create an event and get tickets from the correct Ticket Seller" in {
      import com.goticks.actor.BoxOffice._

      val boxOffice = testKit.spawn(BoxOffice())
      val eventName = EventName("RHCP")
      val probe1    = testKit.createTestProbe[EventCreated]
      boxOffice ! CreateEvent(eventName, EventTickets(10), probe1.ref)
      probe1.expectMessage(EventCreatedSuccessful(Event(eventName, EventTickets(10))))

      val probe2 = testKit.createTestProbe[TicketsGot]
      boxOffice ! GetTickets(eventName, EventTickets(1), probe2.ref)
      probe2.expectMessage(TicketsGotSuccessful(Tickets(eventName, Seq(Ticket(TicketId(1))))))

      boxOffice ! GetTickets(EventName("DavidBowie"), EventTickets(1), probe2.ref)
      probe2.expectMessage(SoldOut)
    }

    "Create a child actor when an event is created and sends it a Tickets message" in {
      import com.goticks.actor.BoxOffice._

      val probe1                                   = testKit.createTestProbe[TicketSeller.Command]
      implicit val creator: TicketSellerRefCreator = (_: ActorContext[Command], _: CreateEvent, _: String) => probe1.ref

      val boxOffice: ActorRef[BoxOffice.Command] = testKit.spawn(BoxOffice())

      val tickets         = EventTickets(3)
      val eventName       = EventName("RHCP")
      val expectedTickets = (1 to tickets.value).map(TicketId).map(Ticket)
      val probe2          = testKit.createTestProbe[BoxOffice.EventCreated]

      boxOffice ! CreateEvent(eventName, tickets, probe2.ref)
      probe1.expectMessage(TicketSeller.Add(expectedTickets))
      probe2.expectMessage(EventCreatedSuccessful(Event(eventName, tickets)))
    }

  }

}
