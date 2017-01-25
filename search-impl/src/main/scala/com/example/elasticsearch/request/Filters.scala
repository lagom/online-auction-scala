package com.example.elasticsearch.request

import com.example.auction.item.api.ItemStatus
import play.api.libs.json.{Format, JsResult, JsValue, Json}


sealed trait Filter

object Filter {
  implicit val format: Format[Filter] = new Format[Filter] {
    override def reads(json: JsValue): JsResult[Filter] = ???

    override def writes(filter: Filter): JsValue =
      filter match {
        case x: ItemStatusFilter => Json.toJson(x)(ItemStatusFilter.format)
        case x: KeywordsFilter => Json.toJson(x)(KeywordsFilter.format)
        case x: PriceRangeFilter => Json.toJson(x)(PriceRangeFilter.format)
        case x: CurrencyFilter => Json.toJson(x)(CurrencyFilter.format)
      }
  }
}

object Filters {

  val STATUS_CREATED = ItemStatusFilter(ItemStatus.Created)

  def keywords(keywords: Option[String]): Option[Filter] = keywords.map(kw => KeywordsFilter(kw))

  def maxPrice(maxPrice: Option[Int], currency: Option[String]): Seq[Option[Filter]] = {
    Seq(
      maxPrice.map(mp => PriceRangeFilter(RangeInt(Some(mp)))),
      currency.map(CurrencyFilter.apply)
    )
  }

}

case class ItemStatusFilter(itemStatus: ItemStatus.Status) extends Filter

case class KeywordsFilter(multi_match: Match) extends Filter

case class PriceRangeFilter(price: RangeInt) extends Filter

case class CurrencyFilter(`match`: CurrencyMatch) extends Filter


object KeywordsFilter {
  def apply(kw: String) = new KeywordsFilter(KeywordMatch(kw))

  implicit val format: Format[KeywordsFilter] = Json.format
}

object CurrencyFilter {
  def apply(currencyId: String): CurrencyFilter = new CurrencyFilter(CurrencyMatch(currencyId))

  implicit val format: Format[CurrencyFilter] = Json.format
}

object ItemStatusFilter {
  implicit val format: Format[ItemStatusFilter] = Json.format
}

object PriceRangeFilter {
  implicit val format: Format[PriceRangeFilter] = Json.format
}

// -------------------------------------------------------------------------------------------


case class RangeInt(lte: Option[Int], gte: Option[Int] = None)

object RangeInt {
  implicit val format: Format[RangeInt] = Json.format
}


// -------------------------------------------------------------------------------------------

sealed trait Match

object Match {
  implicit val format: Format[Match] = new Format[Match] {
    override def reads(json: JsValue): JsResult[Match] = ???

    override def writes(m: Match): JsValue =
      m match {
        case x: KeywordMatch => Json.toJson(x)(KeywordMatch.format)
        case x: CurrencyMatch => Json.toJson(x)(CurrencyMatch.format)
      }
  }

}

case class KeywordMatch(query: String, fields: Seq[String] = Seq("title", "description")) extends Match

object KeywordMatch {
  implicit val format: Format[KeywordMatch] = Json.format
}

case class CurrencyMatch(currencyId: String) extends Match

object CurrencyMatch {
  implicit val format: Format[CurrencyMatch] = Json.format
}
