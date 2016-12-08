package controllers

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Date, UUID}

import com.example.auction.user.api.{User, UserService}
import org.ocpsoft.prettytime.PrettyTime
import org.ocpsoft.prettytime.impl.{ResourcesTimeFormat, ResourcesTimeUnit}
import org.ocpsoft.prettytime.units.JustNow
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Controller, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractController(messagesApi: MessagesApi, userService: UserService)(implicit ec: ExecutionContext) extends Controller {
  protected def withUser[T](block: Option[UUID] => T)(implicit rh: RequestHeader): T = {
    block(rh.session.get("user").map(UUID.fromString))
  }

  protected def requireUser(block: UUID => Future[Result])(implicit rh: RequestHeader): Future[Result] = withUser {
    case Some(user) => block(user)
    case None => Future.successful(Redirect(routes.Main.index))
  }

  protected def loadNav(userId: UUID)(implicit rh: RequestHeader): Future[Nav] = {
    loadNav(Some(userId))
  }

  protected def loadNav(userId: Option[UUID])(implicit rh: RequestHeader): Future[Nav] = {
    userService.getUsers.invoke().map { users =>
      val user = userId.flatMap(uid => users.find(_.id == uid))
      Nav(messagesApi.preferred(rh), users, user)
    }
  }
}

object Nav {
  // todo - make these based on users language/timezone
  val prettyTime: PrettyTime = new PrettyTime
  val zoneId: ZoneId = ZoneId.systemDefault
  val todayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

  prettyTime.removeUnit(classOf[JustNow])
  val justNow: ResourcesTimeUnit = new ResourcesTimeUnit() {
    protected def getResourceKeyPrefix: String = "JustNow"
  }
  prettyTime.registerUnit(justNow, new ResourcesTimeFormat(justNow))

}

case class Nav(messages: Messages, users: Seq[User], user: Option[User]) {

  def formatInstant(instant: Instant): String = {
    val duration = Duration.between(instant, Instant.now)
    if (duration.abs.compareTo(Duration.of(1, ChronoUnit.HOURS)) < 0) {
      Nav.prettyTime.format(Date.from(instant))
    } else {
      val dateTime: ZonedDateTime = ZonedDateTime.ofInstant(instant, Nav.zoneId)
      val today: ZonedDateTime = ZonedDateTime.now(Nav.zoneId).withHour(0).withMinute(0).withSecond(0)
      val tomorrow: ZonedDateTime = today.plus(1, ChronoUnit.DAYS)
      if (dateTime.compareTo(today) > 0 && dateTime.compareTo(tomorrow) < 0) {
        Nav.todayFormatter.format(dateTime)
      } else {
        Nav.dateFormatter.format(dateTime)
      }
    }
  }


  def formatDuration(duration: Duration): String = {
    def isWholeNumberOf(unit: ChronoUnit) = duration.getSeconds % unit.getDuration.getSeconds == 0

    duration match {
      case weeks if isWholeNumberOf(ChronoUnit.WEEKS) =>
        messages("durationWeeks", duration.toDays / 7)
      case days if isWholeNumberOf(ChronoUnit.DAYS) =>
        messages("durationDays", duration.toDays)
      case hours if isWholeNumberOf(ChronoUnit.HOURS) =>
        messages("durationHours", duration.toHours)
      case minutes if isWholeNumberOf(ChronoUnit.MINUTES) =>
        messages("durationMinutes", duration.toMinutes)
      case other =>
        messages("durationSeconds", duration.getSeconds)
    }
  }
}
