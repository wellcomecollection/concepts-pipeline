package weco.concepts.aggregator

object InvalidArg {
  def apply[T](arg: T): InvalidArg =
    InvalidArg(
      s"""Aggregator called with invalid arg: $arg. Should be {"workId": "$$canonicalid"}"""
    )
}

case class InvalidArg(msg: String) extends RuntimeException(msg)
