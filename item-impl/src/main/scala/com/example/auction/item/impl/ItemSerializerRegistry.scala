package com.example.auction.item.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializerRegistry, JsonSerializer}

object ItemSerializerRegistry extends JsonSerializerRegistry {
  override def serializers = List(
    JsonSerializer[Item],

    JsonSerializer[CreateItem],
    JsonSerializer[StartAuction],
    JsonSerializer[UpdatePrice],
    JsonSerializer[FinishAuction],
    JsonSerializer[GetItem.type],

    JsonSerializer[ItemCreated],
    JsonSerializer[AuctionStarted],
    JsonSerializer[PriceUpdated],
    JsonSerializer[AuctionFinished]
  )
}
