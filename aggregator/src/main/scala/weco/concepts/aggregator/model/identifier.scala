package weco.concepts.aggregator.model
//TODO: Harmonise with ingestor models
case class Identifier(
 value: String,
 identifierType: IdentifierType,
 ontologyType: "Identifier" = "Identifier"
)

sealed trait IdentifierType {
  val id: String
  override def toString: String = id
}

object IdentifierType {
  case object LCSubjects extends IdentifierType {
    val id = "lc-subjects"
  }

  case object MeSH extends IdentifierType {
    val id = "nlm-mesh"
  }

  case object LabelDerived extends IdentifierType {
    val id = "label-derived"
  }
}