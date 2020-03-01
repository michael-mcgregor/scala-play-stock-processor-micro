package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import collection.JavaConverters._
import model._
import service.{LoggerHelper, StockDataIngestService}

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

  def apply(
           i: Double,
           j: Double,
           stockDataIngestService: StockDataIngestService = new StockDataIngestService,
           stocksMap: mutable.Map[StockSymbol, Stock] = mutable.HashMap()
  ): Behavior[GetStocks] = {
    // May want to remove stocks that aren't viewed by any clients...
    Behaviors.logMessages(
      Behaviors.receiveMessage {
        case GetStocks(symbols, replyTo) =>
          val stocks = symbols.map(symbol => {
            val stockData = stockDataIngestService.fetchStockDataWithHistory(symbol.toString).asInstanceOf[yahoofinance.Stock]
            val currentPrice = stockData.getHistory.get(stockData.getHistory().size() - 1).getAdjClose.doubleValue()
            val previousPrice = stockData.getHistory.get(stockData.getHistory().size() - 2).getAdjClose.doubleValue()
            LoggerHelper.logger.info(s"found values $currentPrice, $previousPrice, and ${stockData.getSymbol} ${stockData.getHistory.size()}")
            val asdf: mutable.Buffer[StockPrice] = JavaConverters.asScalaBuffer(stockData.getHistory()).map(m =>
                StockPrice(m.getAdjClose.doubleValue()
              )
            )
            stocksMap.getOrElseUpdate(
              symbol,
              new Stock(
                symbol,
                StockQuote(symbol, StockPrice(previousPrice)),
                StockQuote(symbol, StockPrice(currentPrice)),
                asdf
              )
            )
          })
          replyTo ! Stocks(stocks)
          Behaviors.same
      }
    )
  }
}
