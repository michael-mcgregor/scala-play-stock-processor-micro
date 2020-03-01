package util

import play.api.mvc.RequestHeader
import service.LoggerHelper

trait SameOriginCheck {

  /**
   * Checks that the WebSocket comes from the same origin. Protects against Cross-Site WebSocket Hijacking.
   *
   * @param requestHeader RequestHeader
   *
   * @return
   */
  def sameOriginCheck(requestHeader: RequestHeader): Boolean = {
    requestHeader.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        LoggerHelper.logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        LoggerHelper.logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        LoggerHelper.logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
   * Returns true if the value of the Origin header contains an acceptable value.
   *
   * @param origin String
   *
   * @return
   */
  def originMatches(origin: String): Boolean = {
    origin.contains("localhost:9000") || origin.contains("localhost:19001")
  }
}
