package com.example.auction.utils

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PaginatedSequence[T](
  items: Seq[T],
  page: Int,
  pageSize: Int,
  count: Int
) {

  def isEmpty: Boolean = items.isEmpty
  def isFirst: Boolean = page == 0
  def isLast: Boolean = count <= (page + 1) * pageSize
  def isPaged: Boolean = count > pageSize
  def pageCount: Int = ((count - 1) / pageSize) + 1
}

object PaginatedSequence {
  implicit def format[T: Format]: Format[PaginatedSequence[T]] = {
    (
      (__ \ "items").format[Seq[T]] and
      (__ \ "page").format[Int] and
      (__ \ "pageSize").format[Int] and
      (__ \ "count").format[Int]
    ).apply(PaginatedSequence.apply, unlift(PaginatedSequence.unapply))
  }
}
