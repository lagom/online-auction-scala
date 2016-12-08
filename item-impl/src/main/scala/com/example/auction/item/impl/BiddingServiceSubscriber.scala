package com.example.auction.item.impl

import java.util.UUID

import akka.Done
import akka.stream.scaladsl.Flow
import com.example.auction.bidding.api.{BidEvent, BidPlaced, BiddingFinished, BiddingService}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Future

class BiddingServiceSubscriber(persistentEntityRegistry: PersistentEntityRegistry, biddingService: BiddingService) {

  biddingService.bidEvents.subscribe.atLeastOnce(Flow[BidEvent].mapAsync(1) {
    case b @ BiddingFinished(itemId, winningBid) =>
      entityRef(itemId)
        .ask(FinishAuction(winningBid.map(_.bidder), winningBid.map(_.price)))
    case BidPlaced(itemId, bid) => entityRef(itemId).ask(UpdatePrice(bid.price))
    case other => Future.successful(Done)
  })


  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[ItemEntity](itemId.toString)

}
