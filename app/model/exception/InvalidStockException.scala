package model.exception

final case class InvalidStockException(private val message: String = "",
                                 private val cause: Throwable = None.orNull)
  extends Exception(message, cause)
