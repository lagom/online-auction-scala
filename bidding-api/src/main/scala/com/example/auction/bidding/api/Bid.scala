package com.example.auction.bidding.api

import java.time.Instant
import java.util.UUID

import play.api.libs.json.{Format, Json}

import com.example.auction.utils.JsonFormats._

/**
  * A bid value object.
  *
  * @param bidder The user that placed the bid.
  * @param bidTime The time that the bid was placed.
  * @param price The bid price.
  * @param maximumPrice The maximum bid price.
  */
case class Bid(
  bidder: UUID,
  bidTime: Instant,
  price: Int,
  maximumPrice: Int
)

object Bid {
  implicit val format: Format[Bid] = Json.format
}

/**
  * A request to place a bid.
  *
  * @param maximumBidPrice The maximum bid price.
  */
case class PlaceBid(maximumBidPrice: Int)

object PlaceBid {
  implicit val format: Format[PlaceBid] = Json.format
}

/**
  * The status of the result of placing a bid.
  */
object BidResultStatus extends Enumeration {
  /**
    * The bid was accepted, and is the current highest bid.
    */
  val Accepted,
  /**
    * The bid was accepted, but was outbidded by the maximum bid of the current highest bidder.
    */
  AcceptedOutbid,
  /**
    * The bid was accepted, but is below the reserve.
    */
  AcceptedBelowReserve,
  /**
    * The bid was not at least the current bid plus the increment.
    */
  TooLow,
  /**
    * The auction hasn't started.
    */
  NotStarted,
  /**
    * The auction has already finished.
    */
  Finished,
  /**
    * The auction has been cancelled.
    */
  Cancelled = Value

  type Status = Value

  implicit val format: Format[Status] = enumFormat(BidResultStatus)
}

/**
  * The result of placing a bid.
  *
  * @param currentPrice The current bid price.
  * @param status The status of the result.
  * @param currentBidder The current winning bidder.
  */
case class BidResult(currentPrice: Int, status: BidResultStatus.Status, currentBidder: Option[UUID])

object BidResult {
  implicit val format: Format[BidResult] = Json.format
}