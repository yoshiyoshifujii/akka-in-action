package com.goticks

import scala.concurrent.Future
import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout

object BoxOffice {
  def props(implicit timeout: Timeout) = Props(new BoxOffice)
  def name = "boxOffice"


  case class CreateEvent(name: String, tickets: Int)
  case class GetEvent(name: String)
  case object GetEvents
  case class GetTickets(event: String, tickets: Int)
  case class CancelEvent(name: String)

  case class Event(name: String, tickets: Int)
  case class Events(events: Vector[Event])

  sealed trait EventResponse
  case class EventCreated(event: Event) extends EventResponse
  case object EventExists extends EventResponse

}

class BoxOffice(implicit timeout: Timeout) extends Actor with ActorLogging {
  import BoxOffice._
  import context._

  implicit class PrintMap(val future: Future[Any]) {
    def print = future.map { value =>
      log.debug(s"received returned message $value")
      value
    }
  }

  def createTicketSeller(name: String) =
    // TODO: 1.2. TicketSellerアクターを生成する
    ??? //context.actorOf(TicketSeller.props(name), name)

  def receive = LoggingReceive {
    case CreateEvent(name, tickets) =>
      def create() = {
        val eventTickets = createTicketSeller(name)
        val newTickets = (1 to tickets).map { ticketId =>
          TicketSeller.Ticket(ticketId)
        }.toVector
        // TODO: 1.5. TicketSellerアクターにAddメッセージを送信する
        //eventTickets ! TicketSeller.Add(newTickets)
        // TODO: 1.6. 送信元アクターにEventCreatedメッセージを送信する
        //sender() ! EventCreated(Event(name, tickets))
      }
      context.child(name).fold(create())(_ => sender() ! EventExists)



    case GetTickets(event, tickets) =>
      def notFound() = sender() ! TicketSeller.Tickets(event)
      def buy(child: ActorRef) =
      // TODO: 2.3. TicketSellerアクターにBuyメッセージを送信する(返信先はRestApi)
      ??? //child.forward(TicketSeller.Buy(tickets))

      context.child(event).fold(notFound())(buy)


    case GetEvent(event) =>
      def notFound() =
        // TODO: 3.4. TicketSellerアクターにGetEventメッセージを送信する(返信先はRestApi)
        ??? //sender() ! None
      def getEvent(child: ActorRef) =
        // TODO: 3.5. TicketSellerアクターにGetEventメッセージを送信する(返信先はRestApi)
        ??? //child forward TicketSeller.GetEvent
      context.child(event).fold(notFound())(getEvent)


    case GetEvents =>
      import akka.pattern.ask
      import akka.pattern.pipe

      def getEvents: Iterable[Future[Option[Event]]] =
      // TODO: 3.3. 自アクターにGetEventメッセージを送信する(応答あり)
      ??? //context.children.map { child =>
      //  self.ask(GetEvent(child.path.name)).print.mapTo[Option[Event]]
      //}
      def convertToEvents(f: Future[Iterable[Option[Event]]]): Future[Events] =
        f.map(_.flatten).map(l=> Events(l.toVector))

      // TODO: 3.6. 送信元アクターに取得したEventsメッセージを返す
      //pipe(convertToEvents(Future.sequence(getEvents))) to sender()


    case CancelEvent(event) =>
      def notFound() = sender() ! None
      def cancelEvent(child: ActorRef) =
        // TODO: 4.3. TicketSellerアクターにCancelメッセージを送信する(返信先はRestApi)
        ??? //child forward TicketSeller.Cancel
      context.child(event).fold(notFound())(cancelEvent)
  }
}

