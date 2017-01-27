package com.example.elasticsearch.request

import play.api.libs.json.{Format, JsResult, JsValue, Json}


sealed trait SortField

object SortField {
  import Sorters._
  implicit val sortFieldFormat: Format[SortField] = new Format[SortField] {
    override def reads(json: JsValue): JsResult[SortField] = ???

    override def writes(filter: SortField): JsValue =
      filter match {
        case x: priceAscending => Json.toJson(x)(priceAscending.format)
        case x: auctionEndDescending => Json.toJson(x)(auctionEndDescending.format)
      }
  }
}

object Sorters{
  case class priceAscending(price: String = "asc") extends SortField

  case class auctionEndDescending(auctionEnd: Map[String, String] = Map("order" -> "desc", "unmapped_type" -> "boolean")) extends SortField

  object priceAscending {
    implicit val format: Format[priceAscending] = Json.format
  }

  object auctionEndDescending {
    implicit val format: Format[auctionEndDescending] = Json.format
  }


}