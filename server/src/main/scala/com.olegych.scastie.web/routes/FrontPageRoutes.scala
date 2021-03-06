package com.olegych.scastie.web.routes

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

class FrontPageRoutes(production: Boolean) {

  private val index = getFromResource("public/index.html")

  val routes: Route =
    get(
      concat(
        path("public" / Remaining)(
          path ⇒ getFromResource("public/" + path)
        ),
        pathSingleSlash(index),
        path(Segment ~ Slash.?)(_ => index),
        path(Segment / Segment ~ Slash.?)((_, _) => index),
        path(Segment / Segment / Segment ~ Slash.?)((_, _, _) => index)
      )
    )
}
