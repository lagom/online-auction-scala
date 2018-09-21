package controllers

import java.util.{ Locale, UUID }

import com.example.auction.item.api.ItemStatus
import com.example.auction.user.api.{ CreateUser, UserService }
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class Main(userService: UserService, controllerComponents: ControllerComponents)
  (implicit ec: ExecutionContext)
  extends AbstractAuctionController(userService, controllerComponents) {

  val form = Form(mapping(
    "name" -> nonEmptyText
  )(CreateUserForm.apply)(CreateUserForm.unapply))

  def index = Action.async { implicit rh =>
    withUser(loadNav(_).map { implicit nav =>
      Ok(views.html.index())
    }.recover{
      case e => BadRequest("Oops")
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
        userService.createUser.invoke(CreateUser(createUserForm.name))
          .map { user =>
          Redirect(routes.ProfileController.myItems(ItemStatus.Completed.toString.toLowerCase(Locale.ENGLISH), None))
            .withSession("user" -> user.id.toString)
        }
        .recoverWith {
          case e =>
            withUser(loadNav(_).map { implicit nav =>
              Ok(views.html.createUser(form.withGlobalError("Apologies, we are unable to create a new user at this time. Please try again later.")))
            })
        }
      }
    )
  }

  def currentUser(userId: UUID) = Action { rh =>
    Ok.withSession("user" -> userId.toString)
  }
}

case class CreateUserForm(name: String)
