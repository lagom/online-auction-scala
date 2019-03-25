package com.example.elasticsearch

import akka.Done
import com.example.auction.search.IndexedStore
import com.example.elasticsearch.request.{ItemQuery, UpdateIndexItem}
import com.example.elasticsearch.response.SearchResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticSearchIndexedStore(es: Elasticsearch) extends IndexedStore[SearchResult] {

  private val INDEX_NAME = "auction-items"

  override def store(document: IndexedItem): Future[Done] =
    es.updateIndex(INDEX_NAME, document.itemId).invoke(UpdateIndexItem(document)).map(_ => Done.getInstance())

  override def search(query: ItemQuery): Future[SearchResult] =
    es.search(INDEX_NAME).invoke(query)

}
