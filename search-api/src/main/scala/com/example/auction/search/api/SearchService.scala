package com.example.auction.search.api

import java.util.UUID

import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.Service._
import com.lightbend.lagom.scaladsl.api.transport.Method

trait SearchService extends Service {
  override def descriptor: Descriptor =
    named("search").withCalls(
      restCall(Method.POST, "/api/search?pageNo&pageSize", search _)
    ).withAutoAcl(true)

  def search(pageNo: Int, pageSize: Int): ServiceCall[SearchRequest, SearchResponse]
}

case class SearchRequest(keywords: Option[String], maxPrice: Option[Int], currency: Option[String])

case class SearchResponse(items: Seq[SearchItem], pageSize: Int, pageNo: Int, numResults: Int)

case class SearchItem(id: UUID,
                      creatorId: UUID,
                      title: String,
                      description: String,
                      itemStatus: String,
                      currencyId: String,
                      price: Option[Int]
                      // auctionStart:Option[Instant] // TODO: add auctionStart and AuctionEnd
                      // auctionEnd:Option[Instant] // TODO: add auctionStart and AuctionEnd
                     )

import play.api.libs.json.{Format, Json}


object SearchItem {
    implicit val format: Format[SearchItem] = Json.format
}

object SearchResponse {
    implicit val format: Format[SearchResponse] = Json.format
}

object SearchRequest {
    implicit val format: Format[SearchRequest] = Json.format
}
