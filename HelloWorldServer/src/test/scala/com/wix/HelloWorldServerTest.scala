package com.wix

import akka.actor.{ActorRef, ActorSystem, Props}
import org.specs2.mutable.{Before, SpecificationWithJUnit}
import org.specs2.specification.Scope
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HelloWorldServerTest extends SpecificationWithJUnit {
  "HelloWorldServer" should {

    "return 'hello world!' when sending a get request to '/hello' path" in new ctx {
      private val response: HttpResponse = get("/hello")
      response.entity.asString mustEqual "Hello World!"
    }
  }

  trait ctx extends Scope with Before with SimpleRoutingApp {
    lazy val port = 8060
    implicit val system = ActorSystem("TestEnv")
    // execution context for futures
    import system.dispatcher

    override def before: Unit = {

      lazy val helloWorldHttpServiceActor: ActorRef = system.actorOf(Props[HelloWorldHttpServiceActor])

      startServer(interface = "localhost", port = port) {
        { ctx => helloWorldHttpServiceActor ! ctx }
      }
    }

    private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def get(path: String): HttpResponse = {

      val request: HttpRequest = Get(s"http://localhost:$port$path")
      val eventualHttpResponse: Future[HttpResponse] = pipeline(request)

      Await.result(eventualHttpResponse, 5.seconds)
    }
  }
}