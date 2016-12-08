package controllers

import java.time.{Duration, Instant}
import java.time.temporal.ChronoUnit
import java.util.UUID

import com.example.auction.bidding.api._
import com.example.auction.item.api.{Item, ItemService, ItemStatus}
import com.example.auction.user.api.UserService
import com.example.auction.security.ClientSecurity.authenticate
import org.slf4j.LoggerFactory
import play.api.i18n.MessagesApi
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ItemController(messagesApi: MessagesApi, userService: UserService, itemService: ItemService,
  bidService: BiddingService)(implicit ec: ExecutionContext) extends AbstractController(messagesApi, userService) {

  private val log = LoggerFactory.getLogger(classOf[ItemController])

  def createItemForm = Action.async { implicit rh =>
    requireUser(loadNav(_).map { implicit nav =>
      Ok(views.html.editItem(ItemForm.fill(ItemForm())))
    })
  }

  def createItem = Action.async { implicit rh =>
    requireUser { user =>
      ItemForm.bind(rh).fold(
        errorForm => loadNav(user).map { implicit nav =>
          Ok(views.html.editItem(errorForm))
        },
        itemForm => {
          val item = Item.create(user, itemForm.title, itemForm.description, itemForm.currency.name,
            itemForm.currency.toPriceUnits(itemForm.increment.doubleValue()),
            itemForm.currency.toPriceUnits(itemForm.reserve.doubleValue()),
            Duration.of(itemForm.duration, itemForm.durationUnits)
          )
          itemService.createItem.handleRequestHeader(authenticate(user))
              .invoke(item).map { item =>
            Redirect(routes.ItemController.getItem(item.safeId))
          }
        }
      )
    }
  }

  def getItem(itemId: UUID) = Action.async { implicit rh =>
    doGetItem(itemId, BidForm.form)
  }

  private def doGetItem(itemId: UUID, bidForm: Form[BidForm])(implicit rh: RequestHeader): Future[Result] = {
    requireUser(user => loadNav(user).flatMap { implicit nav =>

      val itemFuture = itemService.getItem(itemId)
        .handleRequestHeader(authenticate(user)).invoke()
      val bidHistoryFuture = bidService.getBids(itemId)
        .handleRequestHeader(authenticate(user)).invoke().recover {
        case e =>
          log.error("Error loading bid history", e)
          Nil
      }

      for {
        item <- itemFuture
        bidHistory <- bidHistoryFuture
      } yield {
        if (item.status == ItemStatus.Created && item.creator != user) {
          Forbidden
        } else {
          val seller = nav.users.find(_.id == item.creator)
          val winner = item.auctionWinner.flatMap { winner =>
            nav.users.find(_.id == winner)
          }
          val currentBidMaximum = bidHistory.lastOption.collect {
            case usersBid if usersBid.bidder == user => usersBid.maximumPrice
          }

          // Ensure current price is consistent with bidding history, since there's a lag between when bids
          // are placed and when the item is updated
          val currentPrice = bidHistory.lastOption.map(_.price).orElse(item.price)
          val pItem = item.copy(price = currentPrice)

          // Ensure that the status is consistent with the end time, since there's a lag between when the
          // auction is supposed to end, and when the bidding service actually ends it.
          val cItem = pItem.auctionEnd match {
            case Some(outOfDate) if outOfDate.isBefore(Instant.now()) && pItem.status == ItemStatus.Auction =>
              pItem.copy(status = ItemStatus.Completed)
            case _ => pItem
          }

          val currency = Currency.valueOf(item.currencyId)
          val bidResult = loadBidResult(rh.flash)
          Ok(views.html.item(cItem, bidForm, anonymizeBids(user, currency, bidHistory), user, currency, seller.get, winner,
            currentBidMaximum, bidResult))
        }
      }
    })
  }

  private def anonymizeBids(userId: UUID, currency: Currency, bids: Seq[Bid]): Seq[AnonymousBid] = {
    val (anonymized, _) = bids.foldLeft((Seq.empty[AnonymousBid], Map.empty[UUID, Int])) {
      case ((results, bidders), bid) =>
        val (newBidders, bidderNumber) = bidders.get(bid.bidder) match {
          case _ if userId == bid.bidder => (bidders, 0)
          case Some(n) => (bidders, n)
          case None =>
            val n = bidders.size + 1
            (bidders + (bid.bidder -> n), n)
        }
        (results :+ AnonymousBid(bid.bidTime, bid.price, bidderNumber, bidderNumber == 0), newBidders)
    }

    anonymized
  }

  private def loadBidResult(flash: Flash): Option[BidResult] = {
    for {
      statusString <- flash.get("bidResultStatus")
      status <- Try(BidResultStatus.withName(statusString)).toOption
      priceString <- flash.get("bidResultPrice")
      price <- Try(priceString.toInt).toOption
    } yield {
      BidResult(price, status, None)
    }
  }

  def startAuction(itemId: UUID) = Action.async { implicit rh =>
    requireUser { user =>
      itemService.startAuction(itemId)
        .handleRequestHeader(authenticate(user))
        .invoke()
        .map { _ =>
          Redirect(routes.ItemController.getItem(itemId))
        }
    }
  }

  def placeBid(itemId: UUID) = Action.async { implicit rh =>
    requireUser { user =>
      val form = BidForm.form.bindFromRequest()

      form.fold(
        doGetItem(itemId, _),
        bidForm => {
          val bidPrice = bidForm.currency.toPriceUnits(bidForm.bid.doubleValue())
          bidService.placeBid(itemId)
            .handleRequestHeader(authenticate(user))
            .invoke(PlaceBid(bidPrice))
            .map {
              case BidResult(price, status, _) =>
                Redirect(routes.ItemController.getItem(itemId)).flashing(
                  "bidResultStatus" -> status.toString,
                  "bidResultPrice" -> price.toString
                )
            }
        }
      )
    }
  }
}

object FormMappings {
  val currency = nonEmptyText
    .verifying("invalid.currency", c => Currency.isDefined(c))
    .transform[Currency](Currency.valueOf, _.name)
}

case class ItemForm(
  id: Option[UUID] = None,
  title: String = "",
  description: String = "",
  currency: Currency = Currency.USD,
  increment: BigDecimal = BigDecimal.decimal(0.5),
  reserve: BigDecimal = BigDecimal.exact(0),
  duration: Int = 10,
  durationUnits: ChronoUnit = ChronoUnit.SECONDS
)

object ItemForm {
  import FormMappings._

  private val form = Form(
    mapping(
      "id" -> optional(
        text.verifying("invalid.id", id => Try(UUID.fromString(id)).isSuccess)
          .transform[UUID](UUID.fromString, _.toString)
      ),
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "currency" -> currency,
      "increment" -> bigDecimal
        .verifying("invalid.increment", _ > 0),
      "reserve" -> bigDecimal
        .verifying("invalid.reserve", _ >= 0),
      "duration" -> number
        .verifying("invalid.duration", _ > 0),
      "durationUnits" -> nonEmptyText
        .verifying("invalid.units", du => Try(ChronoUnit.valueOf(du)).isSuccess)
        .transform[ChronoUnit](ChronoUnit.valueOf, _.name)
    )(ItemForm.apply)(ItemForm.unapply)
  )

  def fill(itemForm: ItemForm): Form[ItemForm] = form.fill(itemForm)

  def bind(implicit request: Request[AnyContent]): Form[ItemForm] = {
    val boundForm = form.bindFromRequest()
    boundForm.fold(identity, itemForm => {
      Seq(
        {
          if (!itemForm.currency.isValidStep(itemForm.increment.doubleValue())) {
            Some(FormError("increment", "invalid.step"))
          } else None
        }, {
          if (!itemForm.currency.isValidStep(itemForm.reserve.doubleValue())) {
            Some(FormError("reserve", "invalid.step"))
          } else None
        }, {
          // Make sure that the increment and reserve are multiples of 50c - in a real app, this would be more complex
          // and based on currency specific rules, for now we'll assume currencies that have cents.
          if (!(itemForm.increment * 2).isValidInt) {
            Some(FormError("increment", "invalid.increment"))
          } else {
            val incrementInt = (itemForm.increment * 2).toIntExact
            if (incrementInt <= 0) {
              Some(FormError("increment", "invalid.increment"))
            } else if (incrementInt >= 100) {
              Some(FormError("increment", "invalid.increment"))
            } else None
          }
        }, {
          if (!(itemForm.reserve * 2).isValidInt) {
            Some(FormError("reserve", "invalid.reserve"))
          } else {
            val reserveInt = (itemForm.reserve * 2).toIntExact
            if (reserveInt < 0) {
              Some(FormError("reserve", "invalid.reserve"))
            } else if (reserveInt >= 20000) {
              Some(FormError("reserve", "invalid.reserve"))
            } else None
          }
        }, {
          val duration = Duration.of(itemForm.duration, itemForm.durationUnits)
          if (duration.compareTo(Duration.ofDays(7)) > 0) {
            Some(FormError("duration", "invalid.duration"))
          } else if (duration.compareTo(Duration.ofSeconds(10)) < 0) {
            Some(FormError("duration", "invalid.duration"))
          } else None
        }
      ).foldLeft(boundForm) {
        case (form, Some(error)) => form.withError(error)
        case (form, None) => form
      }

    })
  }
}

case class BidForm(bid: BigDecimal, currency: Currency)

object BidForm {
  import FormMappings._

  val form = Form(
    mapping(
      "bid" -> bigDecimal,
      "currency" -> currency
    )(BidForm.apply)(BidForm.unapply)
  )
}

case class AnonymousBid(bidTime: Instant, bidPrice: Int, bidder: Int, isYou: Boolean)

