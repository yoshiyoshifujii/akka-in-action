package com.goticks.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.goticks.actor.BoxOffice
import com.goticks.actor.BoxOffice.{EventsGotFailure, EventsGotSuccessful}
import com.goticks.domain.model.{EventName, EventTickets}

trait RestApi extends BoxOfficeApi with EventMarshalling {
  import akka.http.scaladsl.model.StatusCodes._

  private def eventsRoute: Route =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getEvents) {
            case EventsGotSuccessful(events) =>
              complete(OK -> events)
            case EventsGotFailure(exception) =>
              failWith(exception)
          }
        }
      }
    }

  private def eventRoute: Route =
    pathPrefix("events" / Segment) { name =>
      pathEndOrSingleSlash {
        val eventName = EventName(name)
        post {
          // POST /events/:event
          entity(as[EventDescription]) { eventDescription =>
            onSuccess(createEvent(eventName, eventDescription.eventTickets)) {
              case BoxOffice.EventCreatedSuccessful(event) =>
                complete(Created -> event)
              case BoxOffice.EventExists =>
                val err = Error(s"$eventName event exists already.")
                complete(BadRequest -> err)
            }
          }
        } ~
          get {
            // GET /events/:event
            onSuccess(getEvent(eventName)) {
              _.event.fold(complete(NotFound))(event => complete(OK -> event))
            }
          } ~
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(eventName)) {
              _.event.fold(complete(NotFound))(event => complete(OK -> event))
            }
          }
      }
    }

  private def ticketsRoute: Route =
    pathPrefix("events" / Segment / "tickets") { name =>
      pathEndOrSingleSlash {
        val eventName = EventName(name)
        post {
          // POST /events/:event/tickets
          entity(as[TicketRequest]) { request =>
            onSuccess(requestTickets(eventName, request.eventTickets)) {
              case BoxOffice.TicketsGotSuccessful(tickets) =>
                complete(Created -> tickets)
              case BoxOffice.SoldOut =>
                complete(NotFound)
            }
          }
        }
      }
    }

  def routes: Route = eventsRoute ~ eventRoute ~ ticketsRoute

}
