package controllers

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.UUID

import akka.NotUsed
import com.example.auction.item.api.{Item, ItemService, ItemStatus}
import com.example.auction.user.api.{User, UserService}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import org.mockito.Mockito
import org.scalatestplus.play.{BaseOneAppPerTest, FakeApplicationFactory, PlaySpec}
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, ApplicationLoader, Environment}

import scala.concurrent.Future

object Users {
  val foundId: UUID = UUID.randomUUID()

  val successfulGetUser: ServiceCall[NotUsed, User] = (request: NotUsed) => {
    Future.successful(
      User(
        foundId,
        "Some User"
      )
    )
  }

  val successfulGetUsers: ServiceCall[NotUsed, Seq[User]] = (request: NotUsed) => Future.successful(
    Seq(
      User(foundId, "Some User"),
      User(UUID.randomUUID(), "User #2"),
      User(UUID.randomUUID(), "User #3")
    )
  )

}

object Items {
  val foundId: UUID = UUID.randomUUID()

  val successfulGetItem: ServiceCall[NotUsed, Item] = (request: NotUsed) => {
    Future.successful(
      Item(
        Some(foundId),
        Users.foundId,
        "Found Item",
        "A item that was found",
        "USD",
        1,
        2,
        Some(2),
        ItemStatus.Created,
        Duration.ofDays(2),
        Some(Instant.now()),
        Some(Instant.now().plus(2L, ChronoUnit.DAYS)),
        None
      )
    )
  }
}

trait LagomFakeApplicationFactory extends FakeApplicationFactory {

  def fakeApplication(): Application = {
    // Default `mode` is `Mode.Test`
    val environment = Environment.simple()
    val context = ApplicationLoader.createContext(environment)

    new loader.WebGateway(context) with LagomServiceLocatorComponents {

      // Mock UserService
      override lazy val userService: UserService = {
        val userServiceMock = Mockito.mock(classOf[UserService])
        Mockito.when(userServiceMock.getUser(Users.foundId)).thenReturn(Users.successfulGetUser)
        Mockito.when(userServiceMock.getUsers).thenReturn(Users.successfulGetUsers)

        userServiceMock
      }

      // Mock ItemService
      override lazy val itemService: ItemService = {
        val itemServiceMock = Mockito.mock(classOf[ItemService])
        Mockito.when(itemServiceMock.getItem(Items.foundId)).thenReturn(Items.successfulGetItem)
        itemServiceMock
      }
    }.application
  }

}


class ItemControllerSpec extends PlaySpec with BaseOneAppPerTest with LagomFakeApplicationFactory {

  "The web gateway " must {
    "get item that exists" in {
      val itemId = Items.foundId
      val request = FakeRequest(GET, s"/item/${itemId.toString}").withSession("user" -> Users.foundId.toString)
      val item = route(app, request).get

      status(item) mustBe OK
    }
  }
}
