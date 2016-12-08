package com.example.auction.bidding.impl

import java.util.UUID

import akka.Done
import akka.stream.scaladsl.Flow
import com.example.auction.item.api.{ItemEvent, ItemService}
import com.example.auction.item.api
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.Future

class ItemServiceSubscriber(persistentEntityRegistry: PersistentEntityRegistry, itemService: ItemService) {

  itemService.itemEvents.subscribe.atLeastOnce(Flow[ItemEvent].mapAsync(1) {

    case as: api.AuctionStarted =>
      val auction = Auction(
        itemId = as.itemId,
        creator = as.creator,
        reservePrice = as.reservePrice,
        increment = as.increment,
        startTime = as.startDate,
        endTime = as.endDate
      )
      entityRef(as.itemId).ask(StartAuction(auction));

    case api.AuctionCancelled(itemId) =>
      entityRef(itemId).ask(CancelAuction)

    case other =>
      Future.successful(Done)

  })

  private def entityRef(itemId: UUID) = persistentEntityRegistry.refFor[AuctionEntity](itemId.toString)

}
