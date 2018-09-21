package com.example.auction.item.impl

import java.time.Duration
import java.util.UUID

import akka.stream.scaladsl.Sink
import com.example.auction.item.api
import com.example.auction.item.api.{ ItemService, ItemSummary }
import com.example.auction.security.ClientSecurity._
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LocalServiceLocator }
import com.lightbend.lagom.scaladsl.testkit.{ ServiceTest, TestTopicComponents }
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

class ItemServiceImplIntegrationTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with ItemComponents with LocalServiceLocator with AhcWSComponents with TestTopicComponents {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ Configuration.from(Map(
          "cassandra-query-journal.eventual-consistency-delay" -> "0"
        ))
    }
  }

  val itemService = server.serviceClient.implement[ItemService]

  import server.materializer

  override def afterAll = server.stop()

  "The Item service" should {

    "allow creating items" in {
      val creatorId = UUID.randomUUID
      for {
        created <- createItem(creatorId, sampleItem(creatorId))
        retrieved <- retrieveItem(created)
      } yield {
        created should ===(retrieved)
      }
    }

    "return all items for a given user" in {
      val tom = UUID.randomUUID
      val jerry = UUID.randomUUID
      val tomItem = sampleItem(tom)
      val jerryItem = sampleItem(jerry)
      (for {
        _ <- createItem(jerry, jerryItem)
        createdTomItem <- createItem(tom, tomItem)
      } yield {
        awaitSuccess() {
          for {
            items <- itemService.getItemsForUser(tom, api.ItemStatus.Created, None).invoke()
          } yield {
            items.count should ===(1)
            items.items should contain only ItemSummary(createdTomItem.safeId, tomItem.title, tomItem.currencyId,
              tomItem.reservePrice, tomItem.status)
          }
        }
      }).flatMap(identity)
    }

    "emit auction started event" in {
      val creatorId = UUID.randomUUID
      for {
        createdItem <- createItem(creatorId, sampleItem(creatorId))
        _ <- startAuction(creatorId, createdItem)
        events: Seq[api.ItemEvent] <- itemService.itemEvents.subscribe.atMostOnceSource
          .filter(_.itemId == createdItem.safeId)
          .take(2)
          .runWith(Sink.seq)
      } yield {
        events.size shouldBe 2
        events.head shouldBe an[api.ItemUpdated]
        events.drop(1).head shouldBe an[api.AuctionStarted]
      }
    }
  }

  private def sampleItem(creatorId: UUID) = {
    api.Item.create(creatorId, "title", "description", "USD", 10, 10, Duration.ofMinutes(10))
  }

  private def createItem(creatorId: UUID, createItem: api.Item) = {
    itemService.createItem.handleRequestHeader(authenticate(creatorId)).invoke(createItem)
  }

  private def retrieveItem(item: api.Item) = {
    itemService.getItem(item.safeId).invoke
  }

  private def startAuction(creatorId: UUID, createdItem: api.Item) = {
    itemService.startAuction(createdItem.safeId).handleRequestHeader(authenticate(creatorId)).invoke
  }

  def awaitSuccess[T](maxDuration: FiniteDuration = 10.seconds, checkEvery: FiniteDuration = 100.milliseconds)(block: => Future[T]): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(): Future[T] = {
      block.recoverWith {
        case recheck if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck())
          }(server.executionContext)
          timeout.future
      }
    }

    doCheck()
  }
}
