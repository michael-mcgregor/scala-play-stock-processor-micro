package model

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * A repository for stockIndex.
 *
 * @param dbConfigProvider The Play db config provider. Play will inject this for you.
 */
@Singleton
class StockRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
   * Here we define the table. It will have a name of stocks
   */
  private class StockTable(tag: Tag) extends Table[StockPercona](tag, "stocks") {

    /** The ID column, which is the primary key */
    def id = column[Long]("id", O.AutoInc)

    /** The ID column, which is the primary key */
    def symbol = column[String]("symbol", O.PrimaryKey)

    /** The name column */
    def name = column[String]("name")

    def currentPrice = column[BigDecimal]("current_price")

    /**
     * This is the tables default "projection".
     *
     * It defines how the columns are converted to and from the Stock object.
     *
     * In this case, we are simply passing the id, name and page parameters to the Stock case classes
     * apply and unapply methods.
     */
    def * = (id, symbol, name, currentPrice) <> ((StockPercona.apply _).tupled, StockPercona.unapply)
  }

  /**
   * The starting point for all queries on the stocks table.
   */
  private val stocks = TableQuery[StockTable]

  /**
   * Create a stock with the given id and name.
   *
   * This is an asynchronous operation, it will return a future of the created stock, which can be used to obtain the
   * id for that stock.
   */
  def create(symbol: String, name: String, currentPrice: BigDecimal): Future[StockPercona] = db.run {
    // We create a projection of just the id and name columns
    (stocks.map(p => (p.symbol, p.name, p.currentPrice))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning stocks.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((symbolNamePrice, id) => StockPercona(id, symbolNamePrice._1, symbolNamePrice._2, symbolNamePrice._3))
      // And finally, insert the stock into the database
      ) += (symbol, name, currentPrice)
  }

  def delete(symbol: String): Future[Int] = db.run {
    stocks.filter(_.symbol === symbol).delete
  }

  /**
   * List all the stocks in the database.
   */
  def list(): Future[Seq[StockPercona]] = db.run {
    stocks.result
  }

  def getStock(symbol: String): Future[StockPercona] = db.run {
    stocks.filter(_.symbol === symbol).result.head
  }
}
