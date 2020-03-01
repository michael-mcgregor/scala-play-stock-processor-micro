package model

import play.api.libs.json._

/**
 * case class that represents the data object being stored into mysql
 *
 * @param id Long
 * @param symbol String
 * @param name String
 * @param currentPrice String
 */
case class StockSql(id: Long, symbol: String, name: String, currentPrice: BigDecimal)

object StockSql {
  implicit val stockFormat = Json.format[StockSql]
}

