package com.example.auction.user.api

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}
import com.example.auction.utils.JsonFormats._

trait UserService extends Service {
  def createUser: ServiceCall[CreateUser, User]
  def getUser(userId: UUID): ServiceCall[NotUsed, User]

  // Remove once we have a proper user service
  def getUsers: ServiceCall[NotUsed, Seq[User]]

  def descriptor = {
    import Service._
    named("user").withCalls(
      pathCall("/api/user", createUser),
      pathCall("/api/user/:id", getUser _),
      pathCall("/api/user", getUsers)
    )
  }
}

case class User(id: UUID, name: String)

object User {
  implicit val format: Format[User] = Json.format
}

case class CreateUser(name: String)

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}