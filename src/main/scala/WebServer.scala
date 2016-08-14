import java.util.UUID

import scala.io.StdIn
import com.twitter.finagle.Http
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.finch._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.finch.circe._

trait Message
final case class Person(name: String, age: Int) extends Message
final case class Envelope[A <: Message](payload: A, server: String, appId: String)

object Envelope {
  private val id = UUID.randomUUID()
  def apply[A <: Message](payload: A): Envelope[A] =
    Envelope(payload, java.net.InetAddress.getLocalHost.toString, id.toString)
}

object WebServer extends TwitterServer {
  def main() = {
    val api: Endpoint[Envelope[Person]] = get("person" :: string) { name: String =>
      Ok(Envelope(Person(name, 41)))
    }
    val server = Http.server.serve(":29002", api.toService)
    onExit { val _ = server.close() }
    Await.ready(adminHttpServer)
  }
}
