package com.wix

import akka.actor.{ActorSystem, Props}
import org.specs2.mutable.{BeforeAfter, SpecificationWithJUnit}
import org.specs2.specification.Scope
import spray.client.pipelining._
import spray.http.{DateTime, HttpRequest, HttpResponse}
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HelloWorldServerTest extends SpecificationWithJUnit {
  sequential

  "HelloWorldServer" should {
    "return 'hello world!' when sending a get request to '/hello' path" in new ctx {
      private val response: HttpResponse = get("/hello")
      response.entity.asString mustEqual "Hello World!"
    }

    "return day time greeting suitable for day time on '/daytime-greeting' path" in new ctx {
      override def before: Unit = {
        val dayTimeTeller = constantTimeTeller(hour = 16)
        setUpServer(Some(dayTimeTeller))
      }

      private val response: HttpResponse = get("/daytime-greeting")
      response.entity.asString mustEqual "Good Day"
    }

    "return day time greeting suitable for morning time on '/daytime-greeting' path" in new ctx {
      override def before: Unit = {
        val morningTimeTeller = constantTimeTeller(hour = 9)
        setUpServer(Some(morningTimeTeller))
      }

      private val response: HttpResponse = get("/daytime-greeting")
      response.entity.asString mustEqual "Good Morning"
    }
  }

  trait ctx extends Scope with SimpleRoutingApp with BeforeAfter {
    lazy val port = 8060
    implicit val system = ActorSystem("TestEnv")
    // execution context for futures

    import system.dispatcher

    def setUpServer(maybeTimeTeller: Option[TimeTeller] = None) = {

      def getServerActor: Props = {
        Props(
          maybeTimeTeller.fold
          (new HelloWorldHttpServiceActor())
          (timeTeller => new HelloWorldHttpServiceActor(timeTeller)))
      }

      val helloWorldHttpServiceActor = system.actorOf(getServerActor, name = "handler")

      startServer(interface = "localhost", port = port) {
        { ctx => helloWorldHttpServiceActor ! ctx }
      }
    }

    def constantTimeTeller(hour: Int) = new TimeTeller {
      override def whatsTheTime(): DateTime = DateTime.now.copy(hour = hour)
    }

    override def before: Unit = {
      setUpServer()
    }

    override def after: Unit = {
      // Test code smell -> shouldn't be tested at this level
      system.shutdown()
    }

    private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def get(path: String): HttpResponse = {

      val request: HttpRequest = Get(s"http://localhost:$port$path")
      val eventualHttpResponse: Future[HttpResponse] = pipeline(request)

      Await.result(eventualHttpResponse, 5.seconds)
    }
  }
}