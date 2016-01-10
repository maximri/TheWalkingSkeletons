package com.wix

import spray.http.DateTime
import spray.routing.HttpServiceActor

class HelloWorldHttpServiceActor(timeTeller: TimeTeller = new DateTimeNowTeller) extends HttpServiceActor {

  def receive = runRoute {
    (get & path("hello")) { complete("Hello World!") } ~
      (get & path("daytime-greeting")) { complete(greeting) }
  }

  private def greeting: String = {
    timeTeller.whatsTheTime().hour match {
      case h if h >= 6 && h <= 10 => "Good Morning"
      case _ => "Good Day"
    }
  }
}

trait TimeTeller {
  def whatsTheTime(): DateTime
}

class DateTimeNowTeller extends TimeTeller {
  override def whatsTheTime(): DateTime = DateTime.now
}