package com.example.auction.item.impl

import java.time.{Duration, Instant}
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import play.api.libs.json.{Format, Json}
import com.example.auction.utils.JsonFormats._

class ItemEntity extends PersistentEntity {
  override type Command = ItemCommand
  override type Event = ItemEvent
  override type State = Option[Item]

  override def initialState: Option[Item] = None

  override def behavior: Behavior = {
    case None => notCreated
    case Some(item) if item.status == ItemStatus.Created => created(item)
    case Some(item) if item.status == ItemStatus.Auction => auction(item)
    case Some(item) if item.status == ItemStatus.Completed => completed
    case Some(item) if item.status == ItemStatus.Cancelled => cancelled
  }

  private val getItemCommand = Actions().onReadOnlyCommand[GetItem.type, Option[Item]] {
    case (GetItem, ctx, state) => ctx.reply(state)
  }

  private val notCreated = {
    Actions().onCommand[CreateItem, Done] {
      case (CreateItem(item), ctx, state) =>
        ctx.thenPersist(ItemCreated(item))(_ => ctx.reply(Done))
    }.onEvent {
      case (ItemCreated(item), state) => Some(item)
    }.orElse(getItemCommand)
  }

  private def created(item: Item) = {
    Actions().onCommand[StartAuction, Done] {
      case (StartAuction(userId), ctx, _) =>
        if (item.creator != userId) {
          ctx.invalidCommand("Only the creator of an auction can start it")
          ctx.done
        } else {
          ctx.thenPersist(AuctionStarted(Instant.now()))(_ => ctx.reply(Done))
        }
    }.onEvent {
      case (AuctionStarted(time), Some(item)) => Some(item.start(time))
    }.orElse(getItemCommand)
  }

  private def auction(item: Item) = {
    Actions().onCommand[UpdatePrice, Done] {
      case (UpdatePrice(price), ctx, _) =>
        ctx.thenPersist(PriceUpdated(price))(_ => ctx.reply(Done))
    }.onCommand[FinishAuction, Done] {
      case (FinishAuction(winner, price), ctx, _) =>
        ctx.thenPersist(AuctionFinished(winner, price))(_ => ctx.reply(Done))
    }.onEvent {
      case (PriceUpdated(price), _) => Some(item.updatePrice(price))
      case (AuctionFinished(winner, price), _) => Some(item.end(winner, price))
    }.onReadOnlyCommand[StartAuction, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.orElse(getItemCommand)
  }

  private val completed = {
    Actions().onReadOnlyCommand[UpdatePrice, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.onReadOnlyCommand[FinishAuction, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.onReadOnlyCommand[StartAuction, Done] {
      case (_, ctx, _) => ctx.reply(Done)
    }.orElse(getItemCommand)
  }

  private val cancelled = completed

}
  
object ItemStatus extends Enumeration {
  val Created, Auction, Completed, Cancelled = Value
  type Status = Value
  
  implicit val format: Format[Status] = enumFormat(ItemStatus)
}

case class Item(
  id: UUID,
  creator: UUID,
  title: String,
  description: String,
  currencyId: String,
  increment: Int,
  reservePrice: Int,
  price: Option[Int],
  status: ItemStatus.Status,
  auctionDuration: Duration,
  auctionStart: Option[Instant],
  auctionEnd: Option[Instant],
  auctionWinner: Option[UUID]
) {
  
  def start(startTime: Instant) = {
    assert(status == ItemStatus.Created)
    copy(
      status = ItemStatus.Auction,
      auctionStart = Some(startTime), 
      auctionEnd = Some(startTime.plus(auctionDuration))
    )
  }

  def end(winner: Option[UUID], price: Option[Int]) = {
    assert(status == ItemStatus.Auction)
    copy(
      status = ItemStatus.Completed,
      price = price,
      auctionWinner = winner
    )
  }

  def updatePrice(price: Int) = {
    assert(status == ItemStatus.Auction)
    copy(
      price = Some(price)
    )
  }

  def cancel = {
    assert(status == ItemStatus.Auction || status == ItemStatus.Completed)
    copy(
      status = ItemStatus.Cancelled
    )
  }
}

object Item {
  implicit val format: Format[Item] = Json.format
}

sealed trait ItemCommand

case object GetItem extends ItemCommand with ReplyType[Option[Item]] {
  implicit val format: Format[GetItem.type] = singletonFormat(GetItem)
}

case class CreateItem(item: Item) extends ItemCommand with ReplyType[Done]
  
object CreateItem {
  implicit val format: Format[CreateItem] = Json.format
}
  
case class StartAuction(userId: UUID) extends ItemCommand with ReplyType[Done]

object StartAuction {
  implicit val format: Format[StartAuction] = Json.format
}

case class UpdatePrice(price: Int) extends ItemCommand with ReplyType[Done]

object UpdatePrice {
  implicit val format: Format[UpdatePrice] = Json.format
}

case class FinishAuction(winner: Option[UUID], price: Option[Int]) extends ItemCommand with ReplyType[Done]

object FinishAuction {
  implicit val format: Format[FinishAuction] = Json.format
}

sealed trait ItemEvent extends AggregateEvent[ItemEvent] {
  override def aggregateTag = ItemEvent.Tag
}

object ItemEvent {
  val NumShards = 4
  val Tag = AggregateEventTag.sharded[ItemEvent](NumShards)
}

case class ItemCreated(item: Item) extends ItemEvent

object ItemCreated {
  implicit val format: Format[ItemCreated] = Json.format
}

case class AuctionStarted(startTime: Instant) extends ItemEvent

object AuctionStarted {
  implicit val format: Format[AuctionStarted] = Json.format
}

case class PriceUpdated(price: Int) extends ItemEvent

object PriceUpdated {
  implicit val format: Format[PriceUpdated] = Json.format
}

case class AuctionFinished(winner: Option[UUID], price: Option[Int]) extends ItemEvent

object AuctionFinished {
  implicit val format: Format[AuctionFinished] = Json.format
}
