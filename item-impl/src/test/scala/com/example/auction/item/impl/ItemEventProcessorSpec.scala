package com.example.auction.item.impl

import java.time.{ Duration, Instant }
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.persistence.query.Sequence
import com.datastax.driver.core.utils.UUIDs
import com.example.auction.item.api
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, Matchers }
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.Future

class ItemEventProcessorSpec extends AsyncWordSpec with BeforeAndAfterAll with Matchers {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra(true)) { ctx =>
    new LagomApplication(ctx) with ItemComponents with AhcWSComponents with LagomKafkaComponents {
      override def serviceLocator = NoServiceLocator
      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver
    }
  }

  override def afterAll() = server.stop()

  private val testDriver = server.application.readSide
  private val itemRepository = server.application.itemRepository
  private val offset = new AtomicInteger()

  private def sampleItem(creatorId: UUID) = {
    Item(UUIDs.timeBased, creatorId, "title", "desc", "USD", 10, 100, None, ItemStatus.Created,
      Duration.ofMinutes(10), None, None, None)
  }

  "The item event processor" should {
    "create an item" in {
      val creatorId = UUID.randomUUID
      val item = sampleItem(creatorId)
      for {
        _ <- feed(item.id, ItemCreated(item))
        items <- getItems(creatorId, api.ItemStatus.Created)
      } yield {
        items.items should contain only
          api.ItemSummary(item.id, item.title, item.currencyId, item.reservePrice, api.ItemStatus.Created)
      }
    }

    "update an item when starting the auction" in {
      val creatorId = UUID.randomUUID
      val item = sampleItem(creatorId)
      for {
        _ <- feed(item.id, ItemCreated(item))
        _ <- feed(item.id, AuctionStarted(Instant.now))
        created <- getItems(creatorId, api.ItemStatus.Created)
        auction <- getItems(creatorId, api.ItemStatus.Auction)
      } yield {
        created.items shouldBe empty
        auction.items should contain only
          api.ItemSummary(item.id, item.title, item.currencyId, item.reservePrice, api.ItemStatus.Auction)
      }
    }

    "ignore price updates" in {
      val creatorId = UUID.randomUUID
      val item = sampleItem(creatorId)
      for {
        _ <- feed(item.id, ItemCreated(item))
        _ <- feed(item.id, AuctionStarted(Instant.now))
        _ <- feed(item.id, PriceUpdated(23))
        auction <- getItems(creatorId, api.ItemStatus.Auction)
      } yield {
        auction.items should contain only
          api.ItemSummary(item.id, item.title, item.currencyId, item.reservePrice, api.ItemStatus.Auction)
      }
    }

    "update an item when finishing the auction" in {
      val creatorId = UUID.randomUUID
      val winnerId = UUID.randomUUID()
      val item = sampleItem(creatorId)
      for {
        _ <- feed(item.id, ItemCreated(item))
        _ <- feed(item.id, AuctionStarted(Instant.now))
        _ <- feed(item.id, AuctionFinished(Some(winnerId), Some(23)))
        auction <- getItems(creatorId, api.ItemStatus.Auction)
        completed <- getItems(creatorId, api.ItemStatus.Completed)
      } yield {
        auction.items shouldBe empty
        completed.items should contain only
          api.ItemSummary(item.id, item.title, item.currencyId, item.reservePrice, api.ItemStatus.Completed)
      }
    }

    "paginate item retrieval" in {
      val creatorId = UUID.randomUUID
      for {
        _ <- Future.sequence(for (i <- 1 to 35) yield {
          val item = sampleItem(creatorId).copy(title = s"title$i")
          feed(item.id, ItemCreated(item))
        })
        items <- itemRepository.getItemsForUser(creatorId, api.ItemStatus.Created, 2, 10)
      } yield {
        items.count should ===(35)
        items.page should ===(2)
        items.pageSize should ===(10)
        items.items should have size 10
        items.items(4).title should ===("title11")
      }

    }
  }

  private def getItems(creatorId: UUID, itemStatus: api.ItemStatus.Status) = {
    itemRepository.getItemsForUser(creatorId, itemStatus, 0, 10)
  }

  private def feed(itemId: UUID, event: ItemEvent) = {
    testDriver.feed(itemId.toString, event, Sequence(offset.getAndIncrement))
  }
}
