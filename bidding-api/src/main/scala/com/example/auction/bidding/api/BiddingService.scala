package com.example.auction.bidding.api

import java.util.UUID

import akka.NotUsed
import com.example.auction.security.SecurityHeaderFilter
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

/**
  * The bidding services.
  *
  * This services manages all bids and lifecycle events associated with them.
  *
  * An auction is created when an AuctionStarted event is received, then bids can be placed, and when the end date
  * specified in AuctionStarted is reached, this service will published a BiddingFinished event with the winning
  * bidder (if there was one).
  */
trait BiddingService extends Service {
  /**
    * A place a bid.
    *
    * @param itemId The item to bid on.
    */
  def placeBid(itemId: UUID): ServiceCall[PlaceBid, BidResult]

  /**
    * Get the bids for an item.
    *
    * @param itemId The item to get the bids for.
    */
  def getBids(itemId: UUID): ServiceCall[NotUsed, Seq[Bid]]

  /**
    * The bid events topic.
    */
  def bidEvents: Topic[BidEvent]

  final override def descriptor = {
    import Service._

    named("bidding").withCalls(
      pathCall("/api/item/:id/bids", placeBid _),
      pathCall("/api/item/:id/bids", getBids _)
    ).withTopics(
      topic("bidding-BidEvent", bidEvents)
    ).withHeaderFilter(SecurityHeaderFilter.Composed)
  }
}
