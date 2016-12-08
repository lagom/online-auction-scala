package com.example.auction.bidding.api

import java.util.UUID

import com.example.auction.utils.JsonFormats._
import play.api.libs.json._
import julienrf.json.derived

/**
  * A bid event.
  */
sealed trait BidEvent {
  val itemId: UUID
}

/**
  * A bid was placed.
  *
  * @param itemId The item the bid was placed on.
  * @param bid The bid itself.
  */
case class BidPlaced(itemId: UUID, bid: Bid) extends BidEvent

object BidPlaced {
  implicit val format: Format[BidPlaced] = Json.format
}

/**
  * Bidding finished.
  *
  * @param itemId The item that finished bidding.
  * @param winningBid The winning bid, if there was one.
  */
case class BiddingFinished(itemId: UUID, winningBid: Option[Bid]) extends BidEvent

object BiddingFinished {
  implicit val format: Format[BiddingFinished] = Json.format
}

object BidEvent {
  implicit val format: Format[BidEvent] =
    derived.flat.oformat((__ \ "type").format[String])
}