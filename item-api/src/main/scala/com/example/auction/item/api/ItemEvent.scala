package com.example.auction.item.api

import java.time.Instant
import java.util.UUID

import julienrf.json.derived
import play.api.libs.json._

sealed trait ItemEvent {
  val itemId: UUID
}

case class ItemUpdated(itemId: UUID, creator: UUID, title: String, description: String, currencyId: String, status: ItemStatus.Status) extends ItemEvent

object ItemUpdated {
  implicit val format: Format[ItemUpdated] = Json.format
}

case class AuctionStarted(itemId: UUID, creator: UUID, reservePrice: Int, increment: Int, startDate: Instant, endDate: Instant) extends ItemEvent

object AuctionStarted {
  implicit val format: Format[AuctionStarted] = Json.format
}

case class AuctionFinished(itemId: UUID, item: Item) extends ItemEvent

object AuctionFinished {
  implicit val format: Format[AuctionFinished] = Json.format
}

case class AuctionCancelled(itemId: UUID) extends ItemEvent

object AuctionCancelled {
  implicit val format: Format[AuctionCancelled] = Json.format
}

object ItemEvent {
  implicit val format: Format[ItemEvent] =
    derived.flat.oformat((__ \ "type").format[String])
}
