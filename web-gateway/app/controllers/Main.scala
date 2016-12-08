package controllers

import java.util.{Locale, UUID}

import com.example.auction.item.api.ItemStatus
import com.example.auction.user.api.{CreateUser, UserService}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.Action

import scala.concurrent.ExecutionContext

class Main(messagesApi: MessagesApi, userService: UserService)(implicit ec: ExecutionContext) extends AbstractController(messagesApi, userService) {

  val form = Form(mapping(
    "name" -> nonEmptyText
  )(CreateUserForm.apply)(CreateUserForm.unapply))

  def index = Action.async { implicit rh =>
    withUser(loadNav(_).map { implicit nav =>
      Ok(views.html.index())
    })
  }

  def createUserForm = Action.async { implicit rh =>
    withUser(loadNav(_).map { implicit nav =>
      Ok(views.html.createUser(form))
    })
  }

  def createUser = Action.async { implicit rh =>
    form.bindFromRequest().fold(
      errorForm => {
        withUser(loadNav(_).map { implicit nav =>
          Ok(views.html.createUser(errorForm))
        })
      },
      createUserForm => {
        userService.createUser.invoke(CreateUser(createUserForm.name)).map { user =>
          Redirect(routes.ProfileController.myItems(ItemStatus.Completed.toString.toLowerCase(Locale.ENGLISH), None, None))
            .withSession("user" -> user.id.toString)
        }
      }
    )
  }

  def currentUser(userId: UUID) = Action { rh =>
    Ok.withSession("user" -> userId.toString)
  }
}

case class CreateUserForm(name: String)
