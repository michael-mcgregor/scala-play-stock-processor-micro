package controllers

import actors._
import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl._
import akka.util.Timeout
import javax.inject.Inject
import org.h2.jdbc.JdbcSQLException
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents, RequestHeader, WebSocket}
import service.{LoggerHelper, SlickAdapterService, StockDataIngestService}
import util.SameOriginCheck
import yahoofinance.Stock

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This class creates the actions and the websocket needed.
 */
class StockController @Inject()(userParentActor: ActorRef[UserParentActor.Create],
                                cc: MessagesControllerComponents,
                                slickAdapterService: SlickAdapterService,
                                stockDataIngestService: StockDataIngestService)
                              (implicit ec: ExecutionContext, scheduler: Scheduler)
  extends MessagesAbstractController(cc) with SameOriginCheck {

  /**
   * The mapping for the stock form.
   */
  val stockForm: Form[StockForm] = Form {
    mapping(
      "id" -> nonEmptyText,
      "name" -> optional(nonEmptyText),
      "currentValue" -> optional(nonEmptyText)
    )(StockForm.apply)(StockForm.unapply)
  }

  /**
   * The index action.
   */
  def stockIndex = Action { implicit request =>
    Ok(views.html.index(stockForm))
  }

  /**
   * The add stock action.
   *
   * This is asynchronous, since we're invoking the asynchronous methods on StockRepository.
   */
  def addStockAction = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    stockForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.index(stockForm)))
      },
      // There were no errors in the from, so create the stock.
      stock => {
        val stockData = stockDataIngestService.fetchStock(stock.id)
        if (stockData == null)
          Future (Redirect(routes.StockController.stockIndex).flashing("success" -> "stock doesn't exist"))

        else {
          val stock = stockData.asInstanceOf[Stock]
          val price = stock.getQuote().getPreviousClose

          slickAdapterService.addStock(
            stock.getSymbol.toLowerCase(),
            stock.getName,
            if (price != null) price else -1
          ).map { _ =>
            // If successful, we simply redirect to the index page.
            Redirect(routes.StockController.stockIndex).flashing("success" -> "stock created")
          }.recover {
            case _: JdbcSQLException =>
                Redirect(routes.StockController.stockIndex).flashing("success" -> "stock already being tracked")
          }
        }
      }
    )
  }

  def deleteStockAction = Action.async { implicit request =>
    // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
    stockForm.bindFromRequest.fold(
      // The error function. We return the index page with the error form, which will render the errors.
      // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
      // a future because the person creation function returns a future.
      errorForm => {
        Future.successful(Ok(views.html.index(errorForm)))
      },
      // There were no errors in the from, so create the stock.
      stock => {
        val id: String = stock.id.toLowerCase
        slickAdapterService.deleteStock(stock.id).map { _ =>
          // If successful, we simply redirect to the index page.
          Redirect(routes.StockController.stockIndex).flashing("success" -> "stock.deleted")
        }
      }
    )
  }

  /**
   * A REST endpoint that gets all the stockIndex as JSON.
   */
  def getStocksAction = Action.async { implicit request =>

//    val stocks = stockDataIngestService.batchFetchMultipleStocks(Array("INTC", "BABA", "TSLA", "AIR.PA", "YHOO"))
//    val asdf = stockDataIngestService.fetchStockDataWithHistory("GOOG")
//    val sdf = 3

    slickAdapterService.getStocks().map { stocks =>
      Ok(Json.toJson(stocks))
    }
  }

  def getStockAction = Action(parse.form(stockForm)).async { implicit request =>
    val id = request.body.id

    slickAdapterService.getStock(id).map { st =>
      Ok(Json.toJson(st))
    }.recover {
      case _ => Ok(Json.toJson(""))
    }
  }

    /**
     * Creates a websocket.  `acceptOrResult` is preferable here because it returns a
     * Future[Flow], which is required internally.
     *
     * @return a fully realized websocket.
     */
    def ws: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] {
      case rh if sameOriginCheck(rh) =>
        wsFutureFlow(rh).map { flow =>
          Right(flow)
        }.recover {
          case e: Exception =>
            LoggerHelper.logger.error("Cannot create websocket", e)
            val jsError = Json.obj("error" -> "Cannot create websocket")
            val result = InternalServerError(jsError)
            Left(result)
        }

      case rejected =>
        LoggerHelper.logger.error(s"Request ${rejected} failed same origin check")
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }

  /**
   * Creates a Future containing a Flow of JsValue in and out.
   */
  private def wsFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    // Use guice assisted injection to instantiate and configure the child actor.
    implicit val timeout = Timeout(1.second) // the first run in dev can take a while :-(
    userParentActor.ask(replyTo => UserParentActor.Create(request.id.toString, replyTo))
  }

}

/**
 * The create stock form.
 *
 * Generally for forms, you should define separate objects to your models, since forms very often need to present data
 * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
 * that is generated once it's created.
 */
case class StockForm(id: String, name: Option[String], currentValue: Option[String])
