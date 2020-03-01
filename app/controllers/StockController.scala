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
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents, RequestHeader, WebSocket}
import service.{LoggerHelper, SlickAdapterService, StockDataIngestService}
import util.SameOriginCheck
import yahoofinance.Stock

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller clas for interfacing with the database and web socket
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
   *
   * @return
   */
  def stockIndex: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index(stockForm))
  }

  /**
   * Tries to add add the stock to be tracked. Will return message is stock is already tracked
   * or if the stock doesn't exist.
   *
   * @return
   */
  def addStockAction(): Action[AnyContent] = Action.async { implicit request =>
    stockForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.index(errorForm)))
      },
      stock => {
        val stockData = stockDataIngestService.fetchStock(stock.id)
        if (stockData == null)
          Future (Redirect(routes.StockController.stockIndex).flashing("success" -> "stock doesn't exist"))
        else {
          val stock: Stock = stockData.asInstanceOf[Stock]
          val price: java.math.BigDecimal = stock.getQuote().getPreviousClose

          slickAdapterService.addStock(
            stock.getSymbol.toLowerCase(),
            stock.getName,
            if (price != null) price else -1
          ).map { _ =>
            Redirect(routes.StockController.stockIndex).flashing("success" -> "stock created")
          }.recover {
            case _: JdbcSQLException =>
                Redirect(routes.StockController.stockIndex).flashing("success" -> "stock already being tracked")
          }
        }
      }
    )
  }

  /**
   * Method will delete the stock by id
   *
   * @return
   */
  def deleteStockAction(): Action[AnyContent] = Action.async { implicit request =>
    stockForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.index(errorForm)))
      },
      stock => {
        val id: String = stock.id.toLowerCase()

        slickAdapterService.deleteStock(id).map { _ =>
          Redirect(routes.StockController.stockIndex).flashing("success" -> "stock deleted")
        }
      }
    )
  }

  /**
   * retrieves all the stock info as JSON
   *
   * @return
   */
  def getStocksAction: Action[AnyContent] = Action.async { implicit request =>
    slickAdapterService.getStocks().map { stocks =>
      Ok(Json.toJson(stocks))
    }
  }

  /**
   * retrieves the specific stock info as Json, lookup by symbol
   *
   * @return
   */
  def getStockAction: Action[StockForm] = Action(parse.form(stockForm)).async { implicit request =>
    val id: String = request.body.id

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
 * The create stock form
 */
case class StockForm(id: String, name: Option[String], currentValue: Option[String])
