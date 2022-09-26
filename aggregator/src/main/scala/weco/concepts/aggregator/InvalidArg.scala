package weco.concepts.aggregator
import java.util.{Map => JavaMap}

object InvalidArg {
  def apply(arg: JavaMap[String, String]) =
    new BadIdentifierTypeException(
      s"""Aggregator called with invalid arg: $arg. Should be {"workId": "$$canonicalid"}"""
    )
}

case class InvalidArg(msg: String) extends RuntimeException(msg)
