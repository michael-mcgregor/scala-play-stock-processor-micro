package service

import javax.inject._
import model.{StockRepository, StockSql}

import scala.concurrent.Future

trait SlickAdapter {
  def getStocks(): Future[Seq[StockSql]]
  def getStock(symbol: String): Future[StockSql]
  def addStock(symbol: String, name: String, currentPrice: BigDecimal): Future[StockSql]
  def deleteStock(symbol: String): Future[Int]
}

@Singleton
class SlickAdapterService @Inject()(repo: StockRepository) extends SlickAdapter {
  override def getStocks(): Future[Seq[StockSql]] = {
    repo.list()
  }

  override def getStock(symbol: String): Future[StockSql] = {
    repo.getStock(symbol)
  }

  override def addStock(symbol: String, name: String, currentPrice: BigDecimal): Future[StockSql] = {
    repo.create(symbol, name, currentPrice)
  }

  override def deleteStock(symbol: String): Future[Int] = {
    repo.delete(symbol)
  }
}
