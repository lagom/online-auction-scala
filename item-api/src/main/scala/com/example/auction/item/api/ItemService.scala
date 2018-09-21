package com.example.auction.item.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.example.auction.security.SecurityHeaderFilter
import com.example.auction.utils
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait ItemService extends Service {
  /**
    * Create an item.
    *
    * @return The created item with its ID populated.
    */
  def createItem: ServiceCall[Item, Item]

  /**
    * Start an auction for an item.
    *
    * @param id The id of the item to start the auction for.
    * @return Done if the auction was started.
    */
  def startAuction(id: UUID): ServiceCall[NotUsed, Done]

  /**
    * Get an item with the given ID.
    *
    * @param id The ID of the item to get.
    * @return The item.
    */
  def getItem(id: UUID): ServiceCall[NotUsed, Item]

  /**
    * Get a list of items for the given user.
    *
    * @param id       The ID of the user.
    * @param status   The status of items to return.
    * @param page     The next page represented as string.
    * @return The sequence of items.
    */
  def getItemsForUser(id: UUID, status: ItemStatus.Status, page: Option[String]): ServiceCall[NotUsed, utils.PagingState[ItemSummary]]

  /**
    * The item events stream.
    */
  def itemEvents: Topic[ItemEvent]

  final override def descriptor = {
    import Service._

    named("item").withCalls(
      pathCall("/api/item", createItem),
      restCall(Method.POST, "/api/item/:id/start", startAuction _),
      pathCall("/api/item/:id", getItem _),
      pathCall("/api/item?userId&status&page", getItemsForUser _)
    ).withTopics(
      topic("item-ItemEvent", this.itemEvents)
    ).withHeaderFilter(SecurityHeaderFilter.Composed)
  }
}
