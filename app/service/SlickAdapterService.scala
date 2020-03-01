package service

import javax.inject._
import model.{Stock, StockPercona, StockRepository}

import scala.concurrent.Future

trait SlickAdapter {
  def getStocks(): Future[Seq[StockPercona]]
  def getStock(symbol: String): Future[StockPercona]
  def addStock(symbol: String, name: String, currentPrice: BigDecimal): Future[StockPercona]
  def deleteStock(symbol: String): Future[Int]
}

@Singleton
class SlickAdapterService @Inject()(repo: StockRepository) extends SlickAdapter {
  override def getStocks(): Future[Seq[StockPercona]] = {
    repo.list()
  }

  override def getStock(symbol: String): Future[StockPercona] = {
    repo.getStock(symbol)
  }

  override def addStock(symbol: String, name: String, currentPrice: BigDecimal): Future[StockPercona] = {
    repo.create(symbol, name, currentPrice)
  }

  override def deleteStock(symbol: String): Future[Int] = {
    repo.delete(symbol)
  }
}
