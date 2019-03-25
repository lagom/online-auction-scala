package com.example.auction.search.impl

import com.example.auction.search.IndexedStore
import com.example.auction.search.api.{SearchItem, SearchRequest, SearchResponse, SearchService}
import com.example.elasticsearch.IndexedItem
import com.example.elasticsearch.request._
import com.example.elasticsearch.response._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

class SearchServiceImpl(indexedStore: IndexedStore[SearchResult]) extends SearchService {

  override def search(pageNo: Int, pageSize: Int): ServiceCall[SearchRequest, SearchResponse] = ServiceCall {
    request =>
      val itemQuery = ItemQuery(request.keywords, request.maxPrice, request.currency, pageNo, pageSize)

      indexedStore.search(itemQuery).map {
        queryResult =>
          val items = toApi(queryResult)
          SearchResponse(items, itemQuery.pageSize, itemQuery.pageNumber, queryResult.hits.total)
      }
  }


  private def toApi(searchResult: SearchResult): Seq[SearchItem] = {
    searchResult.hits.hits.map(_.indexedItem).filter {
      ii =>
        ii.creatorId.isDefined &&
          ii.title.isDefined &&
          ii.description.isDefined &&
          ii.currencyId.isDefined
    }.map(toApi)
  }

  private def toApi(ii: IndexedItem): SearchItem = {
    SearchItem(
      ii.itemId,
      ii.creatorId.get,
      ii.title.get,
      ii.description.get,
      ii.status.get,
      ii.currencyId.get,
      ii.price)
  }
}
