package com.example.elasticsearch.request

import com.example.auction.item.api.ItemStatus
import play.api.libs.json._

case class ItemQuery(titleDescriptionQueryString: Option[String],
                     minPrice: Option[Int],
                     currencyId: Option[String],
                     pageNumber: Int,
                     pageSize: Int)

object ItemQuery {
  implicit val format: Format[ItemQuery] = new Format[ItemQuery] {

    override def reads(json: JsValue): JsResult[ItemQuery] = ???

    override def writes(itemQuery: ItemQuery): JsObject = {
      val fromOffset = itemQuery.pageNumber * itemQuery.pageSize

      Json.obj(
        "query" -> Json.obj(
          "bool" -> Json.obj(
            "must_not" -> Json.obj(
              "match" -> Json.obj(
                "status" -> ItemStatus.Created)),
            "must" -> Json.arr(
              Seq(
                itemQuery.titleDescriptionQueryString.map(queryString =>
                  Json.obj("multi_match" -> Json.obj(
                    "query" -> queryString,
                    "fields" -> Json.arr("title", "description")
                  ))),
                itemQuery.minPrice.map(minPrice =>
                  Json.obj("range" -> Json.obj(
                    "price" -> Json.obj("gte" -> minPrice)
                  ))),
                itemQuery.currencyId.map(currencyId =>
                  Json.obj("match" -> Json.obj("currencyId" -> currencyId)))
              ).flatten[JsObject]
            )
          )
        ),
        "from" -> fromOffset,
        "size" -> itemQuery.pageSize,
        "sort" -> Json.arr(
          Json.obj(
            "auctionEnd" ->
              Json.obj(
                "order" -> "desc",
                "unmapped_type" -> "boolean"
              )
          ),
          Json.obj("price" -> "asc")
        )
      )
    }
  }
}
