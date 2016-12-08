package com.example.auction.item.impl

import com.lightbend.lagom.scaladsl.playjson.{SerializerRegistry, Serializers}

class ItemSerializerRegistry extends SerializerRegistry {
  override def serializers = List(
    Serializers[Item],

    Serializers[CreateItem],
    Serializers[StartAuction],
    Serializers[UpdatePrice],
    Serializers[FinishAuction],
    Serializers[GetItem.type],

    Serializers[ItemCreated],
    Serializers[AuctionStarted],
    Serializers[PriceUpdated],
    Serializers[AuctionFinished]
  )
}
