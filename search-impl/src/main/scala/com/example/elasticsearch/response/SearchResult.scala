package com.example.elasticsearch.response

import com.example.elasticsearch.IndexedItem
import play.api.libs.json.{Format, Json}

case class HitResult(_source: IndexedItem) {
  val indexedItem = _source
}

object HitResult {
  implicit val format: Format[HitResult] = Json.format
}

case class Hits(hits: Seq[HitResult], total: Int)

object Hits {
  implicit val format: Format[Hits] = Json.format
}

case class SearchResult(hits: Hits)

object SearchResult {
  implicit val format: Format[SearchResult] = Json.format
}

