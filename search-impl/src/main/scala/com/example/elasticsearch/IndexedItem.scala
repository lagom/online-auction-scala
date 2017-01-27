package com.example.elasticsearch

import java.util.UUID

import com.example.auction.bidding.api.Bid
import com.example.auction.item.api.{Item, ItemStatus}
import play.api.libs.json.{Format, Json}


case class IndexedItem(
                        itemId: UUID,
                        creatorId: Option[UUID] = None,
                        title: Option[String] = None,
                        description: Option[String] = None,
                        currencyId: Option[String] = None,
                        price: Option[Int] = None,
                        status: Option[String] = None,
                        // auctionStart: Option[Instant], // TODO use start/end instant
                        // auctionEnd: Option[Instant],
                        winner: Option[UUID] = None
                      )


object IndexedItem {
  def forItemUpdated(itemId: UUID, creatorId: UUID, title: String, description: String, currencyId: String, status: String) = {
    IndexedItem(
      itemId,
      creatorId = Some(creatorId),
      title = Some(title),
      description = Some(description),
      currencyId = Some(currencyId),
      status = Some(status)
    )
  }

  def forAuctionStart(itemId: UUID, creatorId: UUID
                      // auctionStart: Instant, // TODO use start/end instant
                      // auctionEnd: Instant,
                     ): IndexedItem =
    IndexedItem(itemId,
      creatorId = Some(creatorId),
      status = Some(ItemStatus.Auction.toString))

  def forAuctionFinished(itemId: UUID, item: Item): IndexedItem =
    IndexedItem(itemId,
      creatorId = Some(item.creator),
      status = Some(ItemStatus.Completed.toString),
      title = Some(item.title),
      description = Some(item.description),
      currencyId = Some(item.currencyId),
      price = Some(item.price.getOrElse(0)),
      winner = item.auctionWinner
      // TODO use start/end instant
    )

  def forPrice(itemId: UUID, price: Int): IndexedItem =
    IndexedItem(itemId, price = Some(price))

  def forWinningBid(itemId: UUID, winningBid: Bid): IndexedItem =
    forPrice(itemId, winningBid.price)
      .copy(winner = Some(winningBid.bidder))

  implicit val format: Format[IndexedItem] = Json.format

}
