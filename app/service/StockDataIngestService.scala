package service

import java.util.Calendar

import javax.inject.Singleton
import yahoofinance.YahooFinance
import yahoofinance.histquotes.Interval

import scala.util.{Failure, Success, Try}

trait StockDataIngest {
  def batchFetchMultipleStocks(symbols: Array[String]): Any
  def fetchStockDataWithHistory(symbol: String): Any
  def fetchStock(symbol: String): Any
}

@Singleton
class StockDataIngestService extends StockDataIngest {
  override def batchFetchMultipleStocks(symbols: Array[String]): Any = {
    val response = Try(YahooFinance.get(symbols.asInstanceOf[Array[String]])) match {
      case Success(value) => value
      case Failure(exception) => throw new RuntimeException(exception)
      case _ => throw new RuntimeException()
    }

    response
  }

  override def fetchStockDataWithHistory(symbol: String): Any = {
    val from = Calendar.getInstance
    val to = Calendar.getInstance
    from.add(Calendar.MONTH, -3) // from 3 months ago

    val response = Try(YahooFinance.get(symbol, from, to, Interval.DAILY)) match {
      case Success(value) => value
      case Failure(exception) => throw new RuntimeException(exception)
      case _ => throw new RuntimeException()
    }

    response
  }

  override def fetchStock(symbol: String): Any = {
    val response = Try(YahooFinance.get(symbol)) match {
      case Success(value) => value
      case Failure(exception) => throw new RuntimeException(exception)
      case _ => throw new RuntimeException()
    }

    response
  }
}
