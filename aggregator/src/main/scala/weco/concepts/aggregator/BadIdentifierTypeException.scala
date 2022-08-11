package weco.concepts.aggregator

object BadIdentifierTypeException {
  def apply(identifier: String) =
    new BadIdentifierTypeException(s"Unknown identifierType $identifier")
}
case class BadIdentifierTypeException(msg: String) extends RuntimeException(msg)
