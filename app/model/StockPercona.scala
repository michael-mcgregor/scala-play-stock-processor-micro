package model

import play.api.libs.json._

case class StockPercona(id: Long, symbol: String, name: String, currentPrice: BigDecimal)

object StockPercona {
  implicit val stockFormat = Json.format[StockPercona]
}

