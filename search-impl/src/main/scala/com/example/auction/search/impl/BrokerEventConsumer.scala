package com.example.auction.search.impl

import akka.stream.scaladsl.Flow
import com.example.auction.bidding.api.{BidEvent, BidPlaced, BiddingFinished, BiddingService}
import com.example.auction.item.api._
import com.example.auction.search.IndexedStore
import com.example.elasticsearch.IndexedItem
import com.example.elasticsearch.response.SearchResult


class BrokerEventConsumer(indexedStore: IndexedStore[SearchResult],
                          itemService: ItemService,
                          biddingService: BiddingService
                         ) {

  private val itemEvents = itemService.itemEvents
  private val biddEvents = biddingService.bidEvents
  itemEvents.subscribe.withGroupId("search-service")
    .atLeastOnce(Flow[ItemEvent].map(toDocument).collect { case Some(x) => x }.mapAsync(1)(indexedStore.store))
  biddEvents.subscribe.withGroupId("search-service")
    .atLeastOnce(Flow[BidEvent].map(toDocument).collect { case Some(x) => x }.mapAsync(1)(indexedStore.store))

  def toDocument(event: ItemEvent): Option[IndexedItem] = {
    event match {
      case AuctionStarted(itemId, creator, _, _, startDate, endDate) =>
        Some(IndexedItem.forAuctionStart(itemId, creator)) // , startDate, endDate)) // TODO: use auction start and end dates
      case AuctionFinished(itemId, item) =>
        Some(IndexedItem.forAuctionFinished(itemId, item))
      case ItemUpdated(itemId, creator, title, description, currencyId, status) =>
        Some(IndexedItem.forItemUpdated(itemId, creator, title, description, currencyId, status.toString))
      case _ => None
    }
  }

  def toDocument(event: BidEvent): Option[IndexedItem] = {
    event match {
      case BidPlaced(itemId, bid) => Some(IndexedItem.forPrice(itemId, bid.price))
      case BiddingFinished(itemId, Some(winningBid)) => Some(IndexedItem.forWinningBid(itemId, winningBid))
      case BiddingFinished(itemId, None) => Some(IndexedItem.forPrice(itemId, 0))
      case _ => None
    }
  }

}
