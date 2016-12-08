package com.example.auction.bidding.impl

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Maintains a read side view of all auctions that gets used to schedule FinishBidding events.
  */
class AuctionSchedulerProcessor(readSide: CassandraReadSide, session: CassandraSession)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[AuctionEvent] {

  private var insertAuctionStatement: PreparedStatement = _
  private var deleteAuctionStatement: PreparedStatement = _

  def buildHandler = {
    readSide.builder[AuctionEvent]("auctionSchedulerOffset")
      .setGlobalPrepare(createTable)
      .setPrepare { tag =>
        prepareStatements()
      }.setEventHandler[AuctionStarted](insertAuction)
      .setEventHandler[BiddingFinished.type](deleteAuction)
      .setEventHandler[AuctionCancelled.type](deleteAuction)
      .build()
  }

  private def createTable(): Future[Done] = {
    for {
      _ <- session.executeCreateTable("""
          CREATE TABLE IF NOT EXISTS auctionSchedule (
            itemId uuid,
            endAuction timestamp,
            PRIMARY KEY (itemId)
          )
      """)
      _ <- session.executeCreateTable("""
          CREATE INDEX IF NOT EXISTS auctionScheduleIndex
            on auctionSchedule (endAuction)
      """)
    } yield Done
  }

  private def prepareStatements(): Future[Done] = {
    for {
      insert <- session.prepare("INSERT INTO auctionSchedule(itemId, endAuction) VALUES (?, ?)")
      delete <- session.prepare("DELETE FROM auctionSchedule where itemId = ?")
    } yield {
      insertAuctionStatement = insert
      deleteAuctionStatement = delete
      Done
    }
  }

  private def insertAuction(started: EventStreamElement[AuctionStarted]) = {
    Future.successful(
      List(insertAuctionStatement.bind(UUID.fromString(started.entityId), Date.from(started.event.auction.endTime)))
    )
  }

  private def deleteAuction(event: EventStreamElement[_]) = {
    Future.successful(List(deleteAuctionStatement.bind(UUID.fromString(event.entityId))))
  }

  def aggregateTags = AuctionEvent.Tag.allTags
}

class AuctionScheduler(session: CassandraSession, system: ActorSystem, registry: PersistentEntityRegistry)(implicit val mat: Materializer, ec: ExecutionContext) {
  private val log = LoggerFactory.getLogger(classOf[AuctionScheduler])

  val finishBiddingDelay = system.settings.config.getDuration("auctionSchedulerDelay", TimeUnit.MILLISECONDS).milliseconds
  system.scheduler.schedule(finishBiddingDelay, finishBiddingDelay) {
    checkFinishBidding()
  }


  /**
    * Check whether there are any auctions that are due to finish, and if so, send a command to finish them.
    */
  private def checkFinishBidding() = {
    session.select("SELECT itemId FROM auctionSchedule WHERE endAuction < toTimestamp(now()) allow filtering").runForeach { row =>
      val uuid = row.getUUID("itemId")
      registry.refFor[AuctionEntity](uuid.toString)
        .ask(FinishBidding)
    }.recover {
      case e =>
        log.warn("Error running finish bidding query", e)
        Done
    }
  }
}
