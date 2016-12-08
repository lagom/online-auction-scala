package com.example.auction.item.api

import java.time.Instant
import java.util.UUID

import com.example.auction.utils.JsonFormats._
import play.api.libs.json._
import julienrf.json.derived

sealed trait ItemEvent {
  val itemId: UUID
}

case class AuctionStarted(itemId: UUID, creator: UUID, reservePrice: Int, increment: Int, startDate: Instant, endDate: Instant) extends ItemEvent

object AuctionStarted {
  implicit val format: Format[AuctionStarted] = Json.format
}

case class AuctionFinished(itemId: UUID) extends ItemEvent

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
