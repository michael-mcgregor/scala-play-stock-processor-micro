package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import model._
import service.StockDataIngestService

import scala.collection.{JavaConverters, mutable}

/**
 * This actor contains a set of stocks internally that may be used by
 * all websocket clients.
 */
object StocksActor {
  final case class Stocks(stocks: Set[Stock]) {
    require(stocks.nonEmpty, "Must specify at least one stock!")
  }

  final case class GetStocks(symbols: Set[StockSymbol], replyTo: ActorRef[Stocks])

  /**
   * called when stocks are added/removed
   *
   * @param stockDataIngestService StockDataIngestService
   * @param stocksMap Map[StockSymbol, Stock]
   * @return
   */
  def apply(
           stockDataIngestService: StockDataIngestService = new StockDataIngestService,
           stocksMap: mutable.Map[StockSymbol, Stock] = mutable.HashMap()
  ): Behavior[GetStocks] = {
    // May want to remove stocks that aren't viewed by any clients...
    Behaviors.logMessages(
      Behaviors.receiveMessage {
        case GetStocks(symbols, replyTo) =>
          val stocks = symbols.map(symbol => {
            val stockData = stockDataIngestService.fetchStockDataWithHistory(symbol.toString).asInstanceOf[yahoofinance.Stock]
            val history: mutable.Buffer[StockPrice] = JavaConverters.asScalaBuffer(stockData.getHistory()).map(m =>
                StockPrice(m.getAdjClose.doubleValue()
              )
            )
            stocksMap.getOrElseUpdate(
              symbol,
              new Stock(
                symbol,
                history,
                stockDataIngestService
              )
            )
          })
          replyTo ! Stocks(stocks)
          Behaviors.same
      }
    )
  }
}
