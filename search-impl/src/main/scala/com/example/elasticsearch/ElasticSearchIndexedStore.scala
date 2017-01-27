package com.example.elasticsearch

import akka.Done
import com.example.auction.search.IndexedStore
import com.example.elasticsearch.request.{QueryRoot, UpdateIndexItem}
import com.example.elasticsearch.response.SearchResult

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticSearchIndexedStore(es: Elasticsearch) extends IndexedStore[SearchResult] {

  private val INDEX_NAME = "auction-items"

  override def store(document: IndexedItem): Future[Done] =
    es.updateIndex(INDEX_NAME, document.itemId).invoke(UpdateIndexItem(document)).map(_ => Done.getInstance())

  override def search(query: QueryRoot): Future[SearchResult] =
    es.search(INDEX_NAME).invoke(query)

}
