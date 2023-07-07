package simple

import autowire.{Client, Core, Serializers, Server}
import upickle.default.{Reader, Writer}

import scala.concurrent.Future

object ClientServer {
  trait PickleSerDe extends Serializers[String, Reader, Writer] {
    def read[Result: Reader](p: String): Result = upickle.default.read(p)
    def write[Result: Writer](r: Result): String = upickle.default.write(r)
  }

  trait Router extends Server[String, Reader, Writer] with PickleSerDe {
    def request(path: Seq[String], data: String): Request = Core.Request(path, read[Map[String, String]](data))
  }

  trait Ajaxer extends Client[String, Reader, Writer] with PickleSerDe {
    def makeRequest(path: Seq[String], data: String): Future[String]
    override def doCall(req: Request): Future[String] = makeRequest(req.path, write(req.args))
  }
}
