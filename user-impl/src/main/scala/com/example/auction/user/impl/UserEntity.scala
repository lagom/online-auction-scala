package com.example.auction.user.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.playjson.{Jsonable, SerializerRegistry, Serializers}
import play.api.libs.json.{Format, Json}
import com.example.auction.utils.JsonFormats._
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

class UserEntity extends PersistentEntity {
  override type Command = UserCommand
  override type Event = UserEvent
  override type State = Option[User]
  override def initialState = None

  override def behavior: Behavior = {
    case Some(user) =>
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) => ctx.reply(state)
      }.onReadOnlyCommand[CreateUser, Done] {
        case (CreateUser(name), ctx, state) => ctx.invalidCommand("User already exists")
      }
    case None =>
      Actions().onReadOnlyCommand[GetUser.type, Option[User]] {
        case (GetUser, ctx, state) => ctx.reply(state)
      }.onCommand[CreateUser, Done] {
        case (CreateUser(name), ctx, state) =>
          ctx.thenPersist(UserCreated(name), _ => ctx.reply(Done))
      }.onEvent {
        case (UserCreated(name), state) => Some(User(name))
      }
  }
}

case class User(name: String) extends Jsonable

object User {
  implicit val format: Format[User] = Json.format
}

sealed trait UserEvent extends Jsonable

case class UserCreated(name: String) extends UserEvent

object UserCreated {
  implicit val format: Format[UserCreated] = Json.format
}

sealed trait UserCommand extends Jsonable

case class CreateUser(name: String) extends UserCommand with ReplyType[Done]

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}

case object GetUser extends UserCommand with ReplyType[Option[User]] {
  implicit val format: Format[GetUser.type] = singletonFormat(GetUser)
}

class UserSerializerRegistry extends SerializerRegistry {
  override def serializers = List(
    Serializers[User],
    Serializers[UserCreated],
    Serializers[CreateUser],
    Serializers[GetUser.type]
  )
}