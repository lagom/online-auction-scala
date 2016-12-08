package com.example.auction.user.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.example.auction.user.api
import com.example.auction.user.api.UserService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext

class UserServiceImpl(registry: PersistentEntityRegistry, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) extends UserService {

  private val currentIdsQuery = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  override def createUser = ServiceCall { createUser =>
    val userId = UUID.randomUUID()
    refFor(userId).ask(CreateUser(createUser.name)).map { _ =>
      api.User(userId, createUser.name)
    }
  }

  override def getUser(userId: UUID) = ServiceCall { _ =>
    refFor(userId).ask(GetUser).map {
      case Some(user) =>
        api.User(userId, user.name)
      case None =>
        throw NotFound(s"User with id $userId")
    }
  }

  override def getUsers = ServiceCall { _ =>
    // Note this should never make production....
    currentIdsQuery.currentPersistenceIds()
      .filter(_.startsWith("UserEntity|"))
      .mapAsync(4) { id =>
        val entityId = id.split("\\|", 2).last
        registry.refFor[UserEntity](entityId)
          .ask(GetUser)
          .map(_.map(user => api.User(UUID.fromString(entityId), user.name)))
      }.collect {
        case Some(user) => user
      }
      .runWith(Sink.seq)
  }

  private def refFor(userId: UUID) = registry.refFor[UserEntity](userId.toString)
}
