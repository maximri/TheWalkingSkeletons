package com.wix

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import org.specs2.mutable.{Before, SpecificationWithJUnit}
import org.specs2.specification.Scope
import spray.can.Http
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}
import spray.routing.SimpleRoutingApp

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PingPongServerTest extends SpecificationWithJUnit {

  "HelloWorldServer" should {

    "return 'PONG!' when sending a get request to '/ping' path" in new ctx {
      private val response: HttpResponse = get("/ping")
      response.entity.asString mustEqual "PONG!"
    }
  }

  trait ctx extends Scope with Before with SimpleRoutingApp {
    lazy val port = 8061
    implicit val system = ActorSystem("TestEnv")

    import system.dispatcher

    override def before: Unit = {
      val pingPongControllerActorRef = system.actorOf(Props[PingPongControllerActor], name = "handler")

      IO(Http) ! Http.Bind(pingPongControllerActorRef, interface = "localhost", port = port)
    }

    private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    def get(path: String): HttpResponse = {

      val request: HttpRequest = Get(s"http://localhost:$port$path")
      val eventualHttpResponse: Future[HttpResponse] = pipeline(request)

      Await.result(eventualHttpResponse, 5.seconds)
    }
  }

}