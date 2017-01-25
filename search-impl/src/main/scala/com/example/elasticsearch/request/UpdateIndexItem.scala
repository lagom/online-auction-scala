package com.example.elasticsearch.request

import com.example.elasticsearch.IndexedItem
import play.api.libs.json.{Format, Json}


case class UpdateIndexItem(doc: IndexedItem, doc_as_upsert: Boolean = true)

object UpdateIndexItem {
  implicit val format: Format[UpdateIndexItem] = Json.format
}
