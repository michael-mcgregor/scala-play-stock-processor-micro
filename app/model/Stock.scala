package model

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import service.StockDataIngestService

import scala.collection.mutable
import scala.concurrent.duration._

/**
 * Stock class that is used to pass data to the front end
 *
 * @param symbol StockSymbol
 * @param historicalData Buffer[StockPrice]
 * @param stockDataIngestService StockDataIngestService // not ideal but hard to get around in this case
 */
class Stock(
             val symbol: StockSymbol,
             val historicalData: mutable.Buffer[StockPrice],
             val stockDataIngestService: StockDataIngestService
           ) {
  private val source: Source[StockQuote, NotUsed] = {
    Source.repeat{
      val updatedStockMeta = stockDataIngestService.fetchStock(symbol.toString).asInstanceOf[yahoofinance.Stock]
      val updatedPrice: Double = updatedStockMeta.getQuote().getPrice.doubleValue()
      val next: StockQuote = StockQuote(symbol, StockPrice(updatedPrice))

      next
    }
  }

  /**
   * Sets up the history in the graph from the historical data pulled from the api
   *
   * @param values Buffer[StockPrice]
   * @return
   */
  def history(values: mutable.Buffer[StockPrice]): Source[StockHistory, NotUsed] = {
    source.grouped(values.size).map(_ => {
      new StockHistory(symbol, values)
    }).take(1)
  }

  /**
   * Provides a source that returns a stock quote every 1000 milliseconds.
   *
   * @return
   */
  def update: Source[StockUpdate, NotUsed] = {
    source
      .throttle(elements = 1, per = 1000.millis, maximumBurst = 1, ThrottleMode.shaping)
      .map(sq => new StockUpdate(sq.symbol, sq.price))
  }

  override val toString: String = s"Stock($symbol)"
}

/*******************************************
 * *****************************************
 * Supporting case classes for Stock class
 *******************************************
 *******************************************/

case class StockQuote(symbol: StockSymbol, price: StockPrice)

/**
 * class that holds the symbol for the stock as string
 *
 * @param symbol String
 */
class StockSymbol private (val symbol: String) extends AnyVal {
  override def toString: String = symbol
}

object StockSymbol {
  import play.api.libs.json._ // Combinator syntax

  def apply(raw: String) = new StockSymbol(raw)

  implicit val stockSymbolReads: Reads[StockSymbol] = {
    JsPath.read[String].map(StockSymbol(_))
  }

  implicit val stockSymbolWrites: Writes[StockSymbol] = Writes {
    (symbol: StockSymbol) => JsString(symbol.symbol)
  }
}

/**
 * class that holds the stock value as a double
 *
 * @param price Double
 */
class StockPrice private (val price: Double) extends AnyVal {
  override def toString: String = price.toString
}

object StockPrice {
  import play.api.libs.json._ // Combinator syntax

  def apply(raw: Double):StockPrice = new StockPrice(raw)

  implicit val stockPriceWrites: Writes[StockPrice] = Writes {
    (price: StockPrice) => JsNumber(price.price)
  }
}

/**
 * Used for automatic JSON conversion
 * https://www.playframework.com/documentation/2.8.x/ScalaJson
 *
 * JSON presentation class for stock history
 *
 * @param symbol StockSymbol
 * @param prices Seq[StockPrice]
 */
case class StockHistory(symbol: StockSymbol, prices: Seq[StockPrice])

object StockHistory {
  import play.api.libs.json._ // Combinator syntax

  implicit val stockHistoryWrites: Writes[StockHistory] = new Writes[StockHistory] {
    override def writes(history: StockHistory): JsValue = Json.obj(
      "type" -> "stockhistory",
      "symbol" -> history.symbol,
      "history" -> history.prices
    )
  }
}

/**
 * JSON presentation class for stock update
 *
 * @param symbol StockSymbol
 * @param price StockPrice
 */
case class StockUpdate(symbol: StockSymbol, price: StockPrice)

object StockUpdate {
  import play.api.libs.json._ // Combinator syntax

  implicit val stockUpdateWrites: Writes[StockUpdate] = new Writes[StockUpdate] {
    override def writes(update: StockUpdate): JsValue = Json.obj(
      "type" -> "stockupdate",
      "symbol" -> update.symbol,
      "price" -> update.price
    )
  }
}
