package com.example.auction.item.impl

import java.util.UUID

import akka.Done
import akka.stream.Materializer
import com.datastax.driver.core._
import com.example.auction.item.api.ItemSummary
import com.example.auction.item.api
import com.example.auction.utils
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import akka.persistence.cassandra.ListenableFutureConverter

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

private[impl] class ItemRepository(session: CassandraSession)(implicit ec: ExecutionContext, mat: Materializer) {

  def getItemsForUser(creatorId: UUID, status: api.ItemStatus.Status, page: Option[String], fetchSize: Int): Future[utils.PagingState[ItemSummary]] = {
    for {
      count <- countItemsByCreatorInStatus(creatorId, status)
      itemsWithNextPage <- selectItemsByCreatorInStatusWithPaging(creatorId, status, page, fetchSize)
    } yield {
      val items = itemsWithNextPage._1
      val nextPage = itemsWithNextPage._2.getOrElse("")
      utils.PagingState(items, nextPage, count)
    }
  }

  private def countItemsByCreatorInStatus(creatorId: UUID, status: api.ItemStatus.Status) = {
    session.selectOne(s"""
      SELECT
        COUNT(*)
      FROM
        ${ItemRepository.itemSummaryByCreatorAndStatusMV}
      WHERE
        ${ItemRepository.creatorId} = ? AND ${ItemRepository.status} = ?
      ORDER BY
        ${ItemRepository.status} ASC, ${ItemRepository.itemId} DESC
    """, // ORDER BY status is required due to https://issues.apache.org/jira/browse/CASSANDRA-10271
      creatorId, status.toString).map {
      case Some(row) => row.getLong("count").toInt
      case None => 0
    }
  }

  private def selectItemsByCreatorInStatus(creatorId: UUID, status: api.ItemStatus.Status, offset: Int, limit: Int) = {
    session.selectAll(s"""
      SELECT
        *
      FROM
        ${ItemRepository.itemSummaryByCreatorAndStatusMV}
      WHERE
        ${ItemRepository.creatorId} = ? AND ${ItemRepository.status} = ?
      ORDER BY
        ${ItemRepository.status} ASC, ${ItemRepository.itemId} DESC
      LIMIT ?
    """, creatorId, status.toString, Integer.valueOf(limit)).map { rows =>
      rows.drop(offset)
       .map(convertItemSummary)
    }
  }

  /**
    * Motivation: https://discuss.lightbend.com/t/how-to-specify-pagination-for-select-query-read-side/870
    */
  private def selectItemsByCreatorInStatusWithPaging(creatorId: UUID,
                                                     status: api.ItemStatus.Status,
                                                     page: Option[String],
                                                     fetchSize: Int): Future[(Seq[ItemSummary], Option[String])] = {
    val statement = new SimpleStatement(s"""
      SELECT
        *
      FROM
        ${ItemRepository.itemSummaryByCreatorAndStatusMV}
      WHERE
        ${ItemRepository.creatorId} = ? AND ${ItemRepository.status} = ?
      ORDER BY
        ${ItemRepository.status} ASC, ${ItemRepository.itemId} DESC
      """, creatorId, status.toString)

    statement.setFetchSize(fetchSize)

    session.underlying().flatMap(underlyingSession => {

      page.map(pagingStateStr => statement.setPagingState(PagingState.fromString(pagingStateStr)))

      underlyingSession.executeAsync(statement).asScala map (resultSet => {
        val newPagingState = resultSet.getExecutionInfo.getPagingState

        /**
          * @note Check against null due to Java code in `getPagingState` function.
          * @note The `getPagingState` function can return null if there is no next page for this reason nextPage is an
          * Option[String].
          */
        val nextPage: Option[String] = if (newPagingState != null) Some(newPagingState.toString) else None

        val iterator = resultSet.iterator().asScala
        iterator.take(fetchSize).map(convertItemSummary).toSeq -> nextPage
      })
    })
  }

  private def convertItemSummary(item: Row): ItemSummary = {
    ItemSummary(
      item.getUUID(ItemRepository.itemId),
      item.getString(ItemRepository.title),
      item.getString(ItemRepository.currencyId),
      item.getInt(ItemRepository.reservePrice),
      api.ItemStatus.withName(item.getString(ItemRepository.status))
    )
  }
}

private[impl] class ItemEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[ItemEvent] {

  private val insertItemCreatorPromise = Promise[PreparedStatement]
  private def insertItemCreator: Future[PreparedStatement] = insertItemCreatorPromise.future

  private val insertItemSummaryByCreatorPromise = Promise[PreparedStatement]
  private def insertItemSummaryByCreator: Future[PreparedStatement] = insertItemSummaryByCreatorPromise.future

  private val updateItemSummaryStatusPromise = Promise[PreparedStatement]
  private def updateItemSummaryStatus: Future[PreparedStatement] = updateItemSummaryStatusPromise.future

  def buildHandler = {
    readSide.builder[ItemEvent](ItemRepository.itemEventOffset)
        .setGlobalPrepare(createTables)
        .setPrepare(_ => prepareStatements())
        .setEventHandler[ItemCreated](e => insertItem(e.event.item))
        .setEventHandler[AuctionStarted](e => doUpdateItemSummaryStatus(e.entityId, api.ItemStatus.Auction))
        .setEventHandler[AuctionFinished](e => doUpdateItemSummaryStatus(e.entityId, api.ItemStatus.Completed))
        .build
  }

  def aggregateTags = ItemEvent.Tag.allTags

  private def createTables() = {
    for {
      _ <- session.executeCreateTable(s"""
        CREATE TABLE IF NOT EXISTS ${ItemRepository.itemCreatorTable} (
          ${ItemRepository.itemId} timeuuid PRIMARY KEY,
          ${ItemRepository.creatorId} UUID
        )
      """)
      _ <- session.executeCreateTable(s"""
        CREATE TABLE IF NOT EXISTS ${ItemRepository.itemSummaryByCreatorTable} (
          ${ItemRepository.creatorId} UUID,
          ${ItemRepository.itemId} timeuuid,
          ${ItemRepository.title} text,
          ${ItemRepository.currencyId} text,
          ${ItemRepository.reservePrice} int,
          ${ItemRepository.status} text,
          PRIMARY KEY (${ItemRepository.creatorId}, ${ItemRepository.itemId})
        ) WITH CLUSTERING ORDER BY (${ItemRepository.itemId} DESC)
      """)
      _ <- session.executeCreateTable(s"""
        CREATE MATERIALIZED VIEW IF NOT EXISTS ${ItemRepository.itemSummaryByCreatorAndStatusMV} AS
          SELECT * FROM ${ItemRepository.itemSummaryByCreatorTable}
          WHERE ${ItemRepository.status} IS NOT NULL AND ${ItemRepository.itemId} IS NOT NULL
          PRIMARY KEY (${ItemRepository.creatorId}, ${ItemRepository.status}, ${ItemRepository.itemId})
          WITH CLUSTERING ORDER BY (${ItemRepository.status} ASC, ${ItemRepository.itemId} DESC)
      """)
    } yield Done
  }

  private def prepareStatements() = {

    val insertItemCreatorFuture = session.prepare(s"""
        INSERT INTO ${ItemRepository.itemCreatorTable} (
          ${ItemRepository.itemId},
          ${ItemRepository.creatorId}
        ) VALUES (?, ?)
      """)
    insertItemCreatorPromise.completeWith(insertItemCreatorFuture)

    val insertItemSummaryByCreatorFuture = session.prepare(s"""
        INSERT INTO ${ItemRepository.itemSummaryByCreatorTable} (
          ${ItemRepository.creatorId},
          ${ItemRepository.itemId},
          ${ItemRepository.title},
          ${ItemRepository.currencyId},
          ${ItemRepository.reservePrice},
          ${ItemRepository.status}
        ) VALUES (?, ?, ?, ?, ?, ?)
      """)
    insertItemSummaryByCreatorPromise.completeWith(insertItemSummaryByCreatorFuture)

    val updateItemSummaryStatusFuture = session.prepare(s"""
        UPDATE ${ItemRepository.itemSummaryByCreatorTable}
          SET ${ItemRepository.status} = ?
        WHERE
          ${ItemRepository.creatorId} = ? AND ${ItemRepository.itemId} = ?
      """)
    updateItemSummaryStatusPromise.completeWith(updateItemSummaryStatusFuture)

    for {
      _ <- insertItemCreatorFuture
      _ <- insertItemSummaryByCreatorFuture
      _ <- updateItemSummaryStatusFuture
    } yield Done
  }

  private def insertItem(item: Item) = {
    for {
      itemCreator <- doInsertItemCreator(item)
      itemSummaryByCreator <- doInsertItemSummaryByCreator(item)
    } yield List(itemCreator, itemSummaryByCreator)
  }

  private def doInsertItemCreator(item: Item) = {
    insertItemCreator.map { ps =>
      val bindInsertItemCreator = ps.bind()
      bindInsertItemCreator.setUUID(ItemRepository.itemId, item.id)
      bindInsertItemCreator.setUUID(ItemRepository.creatorId, item.creator)
      bindInsertItemCreator
    }
  }

  private def doInsertItemSummaryByCreator(item: Item) = {
    insertItemSummaryByCreator.map { ps =>
      val bindInsertItemSummaryByCreator = ps.bind()
      bindInsertItemSummaryByCreator.setUUID(ItemRepository.creatorId, item.creator)
      bindInsertItemSummaryByCreator.setUUID(ItemRepository.itemId, item.id)
      bindInsertItemSummaryByCreator.setString(ItemRepository.title, item.title)
      bindInsertItemSummaryByCreator.setString(ItemRepository.currencyId, item.currencyId)
      bindInsertItemSummaryByCreator.setInt(ItemRepository.reservePrice, Integer.valueOf(item.reservePrice))
      bindInsertItemSummaryByCreator.setString(ItemRepository.status, item.status.toString)
      bindInsertItemSummaryByCreator
    }
  }

  private def doUpdateItemSummaryStatus(itemId: String, status: api.ItemStatus.Status) = {
    val itemUuid = UUID.fromString(itemId)
    selectItemCreator(itemUuid).flatMap {
      case None => throw new IllegalStateException("No itemCreator found for itemId " + itemId)
      case Some(row) =>
        val creatorId = row.getUUID(ItemRepository.creatorId)
        updateItemSummaryStatus.map { ps =>
          val bindUpdateItemSummaryStatus = ps.bind()
          bindUpdateItemSummaryStatus.setString(ItemRepository.status, status.toString)
          bindUpdateItemSummaryStatus.setUUID(ItemRepository.creatorId, creatorId)
          bindUpdateItemSummaryStatus.setUUID(ItemRepository.itemId, itemUuid)
          List(bindUpdateItemSummaryStatus)
        }
    }
  }

  private def selectItemCreator(itemId: UUID) = {
    session.selectOne(s"""
      SELECT
        *
      FROM
        ${ItemRepository.itemCreatorTable}
      WHERE
        ${ItemRepository.itemId} = ?""", itemId)
  }
}

object ItemRepository {
  // Table
  val itemEventOffset: String = "itemEventOffset"
  val itemCreatorTable: String = "itemCreator"
  val itemSummaryByCreatorTable: String = "itemSummaryByCreator"
  val itemSummaryByCreatorAndStatusMV: String = "itemSummaryByCreatorAndStatus"

  // Fields
  val itemId: String = "itemId"
  val creatorId: String = "creatorId"
  val title: String = "title"
  val currencyId: String = "currencyId"
  val reservePrice: String = "reservePrice"
  val status: String = "status"
}
