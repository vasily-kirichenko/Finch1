import java.sql.Date
import java.util.UUID

import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import io.finch._
import io.circe.generic.auto._
import io.finch.circe._
import com.typesafe.slick.driver.ms.SQLServerDriver.api._
import Utils.Implicits._
import slick.backend.StaticDatabaseConfig

trait Message
final case class Person(name: String, age: Int) extends Message
final case class Envelope[A <: Message](payload: A, server: String, appId: String)

object Envelope {
  private val id = UUID.randomUUID().toString
  private val host: String = java.net.InetAddress.getLocalHost.toString

  def apply[A <: Message](payload: A): Envelope[A] = {
    Envelope(payload, host, id)
  }
}

case class FormatType
  (id: Short,
   name: String,
   isCategorized: Boolean,
   isSafe: Boolean,
   isUnknown: Boolean,
   isObsolete: Boolean,
   idElf: Short)

object FormatTypeOps {
  type Tuple = (Short, String, Boolean, Boolean, Boolean, Boolean, Short)
}

@StaticDatabaseConfig("file:src/main/resources/application.conf#tsql")
object WebServer extends TwitterServer {
  def main() = {
    val person: Endpoint[Envelope[Person]] = get("person" :: string) { name: String => Ok(Envelope(Person(name, 41))) }

    implicit val ctx = concurrent.ExecutionContext.Implicits.global
    val db = Database.forConfig("wl")

    val formatType: Endpoint[FormatType] = get("formatType" :: int) { id: Int =>
      val cmd: DBIO[Option[FormatTypeOps.Tuple]] =
        tsql"""select cast(IDFormatType as smallint), Name, IsCategorized, IsSafe, IsUnknown, IsObsolete, IDElf
               from dbo.FormatType
               where IDFormatType = $id""".headOption

      db.run(cmd).map {
          case Some(x) => Ok(FormatType.tupled(x))
          case None => NotFound(new Exception(s"FormatType was not found for id = $id"))
      }.asTwitter

//      val q = for {
//        ft <- WL.formatTypes if ft.id === id.toShort
//      } yield ft
//
//      val r = db.run(q.result).map {
//        case (id, name, isCategorized, isSafe, isUnknown, isObsolete, idElf) :: _ =>
//          Ok(FormatType(id, name, isCategorized, isSafe, isUnknown, isObsolete, idElf))
//        case x if x.isEmpty => NotFound(new Exception(s"FormatType was not found for id = $id"))
//      }
//
//      r.asTwitter
    }

    val notFound: Endpoint[String] = * {
      Output.payload("Service not found", Status.NotFound)
    }

    val api = (person :+: formatType :+: notFound).handle {
      case e: Exception => BadRequest(e)
    }

    val server = Http.server.serve(":29002", api.toService)
    onExit { val _ = server.close() }
    Await.ready(adminHttpServer)
  }
}
