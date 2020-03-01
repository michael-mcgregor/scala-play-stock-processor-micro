package util

import play.api.mvc.RequestHeader
import service.LoggerHelper

trait SameOriginCheck {

  /**
   * Checks that the WebSocket comes from the same origin.  This is necessary to protect
   * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
   *
   * See https://tools.ietf.org/html/rfc6455#section-1.3 and
   * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
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
   * This is probably better done through configuration same as the allowedhosts filter.
   */
  def originMatches(origin: String): Boolean = {
    origin.contains("localhost:9000") || origin.contains("localhost:19001")
  }
}
