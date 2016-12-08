import play.api.i18n.Messages
import views.html.foundationFieldConstructor
import views.html.helper.FieldConstructor

package object controllers {
  implicit def fieldConstructor: FieldConstructor = FieldConstructor(foundationFieldConstructor.apply)

  implicit def navToMessages(implicit nav: Nav): Messages = {
    nav.messages
  }
}
