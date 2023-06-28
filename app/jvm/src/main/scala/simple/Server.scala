package simple

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Properties

object Server {
  val router: ClientServer.Router = new ClientServer.Router {}

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val port = Properties.envOrElse("PORT", "8080").toInt
    val route =
      get {
        pathSingleSlash {
          complete {
            HttpEntity(ContentTypes.`text/html(UTF-8)`, Page.skeleton.render)
          }
        } ~
        getFromResourceDirectory("")
      } ~
      post {
        path(Segments) { path =>
          entity(as[String]) { data =>
            complete {
              router.route[Api](ApiService)(router.request(path, data))
            }
          }
        }
      }
    Http().bindAndHandle(route, "0.0.0.0", port = port)
    println("Server started.")
  }
}
