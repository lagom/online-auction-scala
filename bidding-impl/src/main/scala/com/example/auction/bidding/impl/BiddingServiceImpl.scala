package com.example.auction.bidding.impl

import java.util.UUID

import com.example.auction.bidding.api
import com.example.auction.bidding.api.BiddingService
import com.example.auction.security.ServerSecurity._
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class BiddingServiceImpl(persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends BiddingService {

  override def placeBid(itemId: UUID) = authenticated(userId => ServerServiceCall { bid =>
    entityRef(itemId).ask(PlaceBid(bid.maximumBidPrice, userId))
      .map { result =>
        val status = result.status match {
          case PlaceBidStatus.Accepted => api.BidResultStatus.Accepted
          case PlaceBidStatus.AcceptedBelowReserve => api.BidResultStatus.AcceptedBelowReserve
          case PlaceBidStatus.AcceptedOutbid => api.BidResultStatus.AcceptedOutbid
          case PlaceBidStatus.Cancelled => api.BidResultStatus.Cancelled
          case PlaceBidStatus.Finished => api.BidResultStatus.Finished
          case PlaceBidStatus.NotStarted => api.BidResultStatus.NotStarted
          case PlaceBidStatus.TooLow => api.BidResultStatus.TooLow
        }

        api.BidResult(result.currentPrice, status, result.currentBidder)
      }
  })

  override def getBids(itemId: UUID) = ServerServiceCall { _ =>
    entityRef(itemId).ask(GetAuction).map { auction =>
      auction.biddingHistory.map(convertBid).reverse
    }
  }

  override def bidEvents = TopicProducer.taggedStreamWithOffset(AuctionEvent.Tag.allTags.to[immutable.Seq]) { (tag, offset) =>
    persistentEntityRegistry.eventStream(tag, offset).filter(e =>
      e.event.isInstanceOf[BidPlaced] || e.event.isInstanceOf[BiddingFinished.type]
    ).mapAsync(1) { event =>
      event.event match {
        case BidPlaced(bid) =>
          val message = api.BidPlaced(UUID.fromString(event.entityId), convertBid(bid))
          Future.successful((message, event.offset))
        case BiddingFinished =>
          persistentEntityRegistry.refFor[AuctionEntity](event.entityId).ask(GetAuction).map { auction =>

            val message = api.BiddingFinished(UUID.fromString(event.entityId),
              auction.biddingHistory.headOption
                .filter(_.bidPrice >= auction.auction.get.reservePrice)
                .map(convertBid))

            (message, event.offset)
          }
      }
    }
  }

  private def convertBid(bid: Bid): api.Bid = api.Bid(bid.bidder, bid.bidTime, bid.bidPrice, bid.maximumBid)

  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[AuctionEntity](itemId.toString)

}
