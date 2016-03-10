package com.wix

import akka.actor.{Actor, _}
import spray.can.Http
import spray.http.HttpMethods._
import spray.http.{HttpRequest, HttpResponse, Uri}

class PingPongControllerActor extends Actor {

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG!")
  }
}