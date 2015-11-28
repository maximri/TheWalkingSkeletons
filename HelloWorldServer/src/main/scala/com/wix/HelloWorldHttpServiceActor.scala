package com.wix


import spray.routing.HttpServiceActor

class HelloWorldHttpServiceActor extends HttpServiceActor {

  def receive = runRoute {
    (get & path("hello")) { complete("Hello World!") }
  }
}
