package controllers

import com.example.auction.search.api.{SearchRequest, SearchService}
import com.example.auction.user.api.UserService
import com.example.auction.utils.PaginatedSequence
import com.typesafe.config.Config
import play.api.data.Forms.{nonEmptyText, _}
import play.api.data.{Form, Mapping}
import play.api.mvc.{ControllerComponents, _}

import scala.concurrent.{ExecutionContext, Future}

class SearchController(config: Config,
                       searchService: SearchService,
                       userService: UserService,
                       controllerComponents: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractAuctionController(userService, controllerComponents) {

  private val showInlineInstruction: Boolean = config.getBoolean("online-auction.instruction.show")
  private val pageSize: Int = config.getInt("items-search.page-size")

  def searchForm(): Action[AnyContent] = Action.async { implicit request =>
    requireUser(loadNav(_).map { implicit nav =>
      Ok(views.html.searchItems(showInlineInstruction = showInlineInstruction,
        form = SearchItemsForm.form.fill(SearchItemsForm()),
        optionalSearchItemPaginatedSequence = None))
    })
  }

  def search(): Action[AnyContent] = Action.async { implicit request =>
    requireUser(user =>
      loadNav(user).flatMap { implicit nav =>
        SearchItemsForm.form.bindFromRequest().fold(
          errorForm => {
            Future.successful(Ok(views.html.searchItems(
              showInlineInstruction = showInlineInstruction,
              form = errorForm,
              optionalSearchItemPaginatedSequence = None)))
          },
          searchItemsForm => {
            searchService.search(searchItemsForm.pageNumber, pageSize)
              .invoke(SearchRequest(
                if (searchItemsForm.keywords.isEmpty) None else Some(searchItemsForm.keywords),
                Some(searchItemsForm.maximumPrice.intValue()),
                Some(searchItemsForm.currency.name)))
              .map(searchResponse => {
                Ok(views.html.searchItems(
                  showInlineInstruction = showInlineInstruction,
                  form = SearchItemsForm.form.fill(searchItemsForm),
                  optionalSearchItemPaginatedSequence = Some(PaginatedSequence(
                    searchResponse.items,
                    searchResponse.pageNo,
                    searchResponse.pageSize,
                    searchResponse.numResults))))
              })
          })
      })
  }
}


case class SearchItemsForm(keywords: String = "",
                           maximumPrice: BigDecimal = 0.0,
                           currency: Currency = Currency.USD,
                           pageNumber: Int = 0)

object SearchItemsForm {
  val currency: Mapping[Currency] = nonEmptyText
    .verifying("invalid.currency", c => Currency.isDefined(c))
    .transform[Currency](Currency.valueOf, _.name)
  val form = Form(mapping(
    "keywords" -> text,
    "maximumPrice" -> bigDecimal,
    "currency" -> currency,
    "pageNumber" -> number(min = 0)
  )(SearchItemsForm.apply)(SearchItemsForm.unapply))
}

