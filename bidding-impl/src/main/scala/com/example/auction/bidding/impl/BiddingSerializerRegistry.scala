package com.example.auction.bidding.impl

import com.lightbend.lagom.scaladsl.playjson.{SerializerRegistry, Serializers}

import scala.collection.immutable.Seq

class BiddingSerializerRegistry extends SerializerRegistry {
  override def serializers: Seq[Serializers[_]] = Seq(
    // State
    Serializers[AuctionState],
    // Commands and replies
    Serializers[GetAuction.type],
    Serializers[StartAuction],
    Serializers[PlaceBid],
    Serializers[PlaceBidResult],
    Serializers[FinishBidding.type],
    Serializers[CancelAuction.type],
    // Events
    Serializers[AuctionStarted],
    Serializers[BidPlaced],
    Serializers[BiddingFinished.type],
    Serializers[AuctionCancelled.type]
  )
}
