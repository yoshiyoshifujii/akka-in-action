package com.goticks.actor

import com.goticks.StopSystemAfterAll
import com.goticks.domain.model._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TicketSellerTest extends AnyWordSpec with Matchers with StopSystemAfterAll {

  "The TicketSeller" must {

    "Sell tickets until they are sold out" in {
      import com.goticks.actor.TicketSeller._

      def mkTickets      = (1 to 10).map(i => Ticket(TicketId(i)))
      val eventName      = EventName("RHCP")
      val ticketingActor = testKit.spawn(TicketSeller(eventName))
      val probe          = testKit.createTestProbe[TicketsGot]

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(EventTickets(1), probe.ref)
      probe.expectMessage(TicketsGot(Tickets(eventName, Seq(Ticket(TicketId(1))))))

      val nrs = 2 to 10
      nrs.foreach(_ => ticketingActor ! Buy(EventTickets(1), probe.ref))

      val tickets = probe.receiveMessages(9)
      tickets.zip(nrs).foreach {
        case (TicketsGot(Tickets(_, Seq(Ticket(id)))), ix) => id.value must be(ix)
      }

      ticketingActor ! Buy(EventTickets(1), probe.ref)
      probe.expectMessage(TicketsGot(Tickets(eventName)))
    }

    "Sell tickets in batches until they are sold out" in {
      import com.goticks.actor.TicketSeller._

      val firstBatchSize = 10

      def mkTickets = (1 to (10 * firstBatchSize)).map(i => Ticket(TicketId(i)))

      val eventName      = EventName("Madlib")
      val ticketingActor = testKit.spawn(TicketSeller(eventName))
      val probe          = testKit.createTestProbe[TicketsGot]

      ticketingActor ! Add(mkTickets)
      ticketingActor ! Buy(EventTickets(firstBatchSize), probe.ref)
      val bought = (1 to firstBatchSize).map(TicketId).map(Ticket)

      probe.expectMessage(TicketsGot(Tickets(eventName, bought)))

      val secondBatchSize = 5
      val nrBatches       = 18

      val batches = (1 to nrBatches * secondBatchSize)
      batches.foreach(_ => ticketingActor ! Buy(EventTickets(secondBatchSize), probe.ref))

      val tickets = probe.receiveMessages(nrBatches)

      tickets.zip(batches).foreach {
        case (TicketsGot(Tickets(_, bought)), ix) =>
          bought.size must equal(secondBatchSize)
          val last  = ix * secondBatchSize + firstBatchSize
          val first = ix * secondBatchSize + firstBatchSize - (secondBatchSize - 1)
          bought.map(_.id.value) must equal(first to last)
      }

      ticketingActor ! Buy(EventTickets(1), probe.ref)
      probe.expectMessage(TicketsGot(Tickets(eventName)))

      ticketingActor ! Buy(EventTickets(10), probe.ref)
      probe.expectMessage(TicketsGot(Tickets(eventName)))

    }

  }

}
