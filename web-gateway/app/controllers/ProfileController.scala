package controllers

import java.util.Locale

import com.example.auction.item.api.{ ItemService, ItemStatus }
import com.example.auction.user.api.UserService
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class ProfileController(
  userService: UserService,
  itemService: ItemService,
  controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
  extends AbstractAuctionController(userService, controllerComponents) {

  def myItems(statusParam: String, page: Option[String]) = Action.async { implicit rh =>
    val status = statusParam.toLowerCase(Locale.ENGLISH) match {
      case "created" => ItemStatus.Created
      case "auction" => ItemStatus.Auction
      case "completed" => ItemStatus.Completed
      case "cancelled" => ItemStatus.Cancelled
      case _ => throw new IllegalArgumentException("Unknown status: " + statusParam)
    }
    requireUser(userId => for {
      nav <- loadNav(userId)
      pagingState <- itemService.getItemsForUser(userId, status, page).invoke()
    } yield {
      Ok(views.html.myItems(status, pagingState)(nav, rh))
    })
  }
}
