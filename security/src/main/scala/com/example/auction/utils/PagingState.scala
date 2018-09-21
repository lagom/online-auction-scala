package com.example.auction.utils

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PagingState[T](
  items: Seq[T],
  nextPage: String,
  count: Int
) {

  def isEmpty: Boolean = items.isEmpty
  def isLast: Boolean = nextPage.isEmpty
}

object PagingState {
  implicit def format[T: Format]: Format[PagingState[T]] = {
    (
      (__ \ "items").format[Seq[T]] and
      (__ \ "nextPage").format[String] and
      (__ \ "count").format[Int]
      ).apply(PagingState.apply, unlift(PagingState.unapply))
  }
}




