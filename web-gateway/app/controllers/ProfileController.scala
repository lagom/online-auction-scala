package controllers

import java.util.Locale

import com.example.auction.item.api.{ItemService, ItemStatus}
import com.example.auction.user.api.UserService
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.ExecutionContext

class ProfileController(messagesApi: MessagesApi, userService: UserService, itemService: ItemService)
  (implicit ec: ExecutionContext) extends AbstractController(messagesApi, userService) {

  def myItems(statusParam: String, page: Option[Int], pageSize: Option[Int]) = Action.async { implicit rh =>
    val status = statusParam.toLowerCase(Locale.ENGLISH) match {
      case "created" => ItemStatus.Created
      case "auction" => ItemStatus.Auction
      case "completed" => ItemStatus.Completed
      case "cancelled" => ItemStatus.Cancelled
      case _ => throw new IllegalArgumentException("Unknown status: " + statusParam)
    }
    requireUser(userId => for {
      nav <- loadNav(userId)
      items <- itemService.getItemsForUser(userId, status, page, pageSize).invoke()
    } yield {
      Ok(views.html.myItems(status, items)(nav))
    })
  }
}