package model

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

/**
 * A repository for stockIndex.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class StockRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  /**
   * table that persists data about the stocks
   *
   * @param tag Tag
   */
  private class StockTable(tag: Tag) extends Table[StockSql](tag, "stocks") {

    /**
     * Id column used for general tracking
     */
    def id: Rep[Long] = column[Long]("id", O.AutoInc)

    /**
     * symbol column the represents the short representation of the stock
     * This is the primary key
     */
    def symbol: Rep[String] = column[String]("symbol", O.PrimaryKey)

    /**
     * full name of the stock
     */
    def name: Rep[String] = column[String]("name")

    /**
     * pulls the current price of the stock
     */
    def currentPrice: Rep[BigDecimal] = column[BigDecimal]("current_price")

    /**
     * This is the tables default "projection".
     */
    def * : ProvenShape[StockSql] = (id, symbol, name, currentPrice) <> ((StockSql.apply _).tupled, StockSql.unapply)
  }

  /**
   * The starting point for all queries on the stocks table.
   */
  private val stocks = TableQuery[StockTable]

  /**
   * Create a stock with the given symbol, name and price.
   *
   * This is an asynchronous operation, it will return a future of the created stock, which can be used to obtain the
   * id for that stock.
   *
   * @param symbol String
   * @param name String
   * @param currentPrice BigDecimal
   *
   * @return
   */
  def create(symbol: String, name: String, currentPrice: BigDecimal): Future[StockSql] = db.run {
    (stocks.map(p => (p.symbol, p.name, p.currentPrice))
      returning stocks.map(_.id)
      into ((symbolNamePrice, id) =>
        StockSql(id, symbolNamePrice._1, symbolNamePrice._2, symbolNamePrice._3)
        )) += (symbol, name, currentPrice)
  }

  /**
   * deletes a record from the database by the id
   *
   * @param symbol String
   *
   * @return
   */
  def delete(symbol: String): Future[Int] = db.run {
    stocks.filter(_.symbol === symbol).delete
  }

  /**
   * list all the stocks in the database
   *
   * @return
   */
  def list(): Future[Seq[StockSql]] = db.run {
    stocks.result
  }

  /**
   * retrieve stock object from mysql by id
   *
   * @param symbol String
   *
   * @return
   */
  def getStock(symbol: String): Future[StockSql] = db.run {
    stocks.filter(_.symbol === symbol).result.head
  }
}
