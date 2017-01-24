package com.example.auction.bidding.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializerRegistry, JsonSerializer}

import scala.collection.immutable.Seq

object BiddingSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // State
    JsonSerializer[AuctionState],
    // Commands and replies
    JsonSerializer[GetAuction.type],
    JsonSerializer[StartAuction],
    JsonSerializer[PlaceBid],
    JsonSerializer[PlaceBidResult],
    JsonSerializer[FinishBidding.type],
    JsonSerializer[CancelAuction.type],
    // Events
    JsonSerializer[AuctionStarted],
    JsonSerializer[BidPlaced],
    JsonSerializer[BiddingFinished.type],
    JsonSerializer[AuctionCancelled.type]
  )
}
