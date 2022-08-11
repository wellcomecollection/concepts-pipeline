package weco.concepts.common.model

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
  case object LCNames extends IdentifierType {
    val id = "lc-names"
  }
  case object MeSH extends IdentifierType {
    val id = "nlm-mesh"
  }
  case object LabelDerived extends IdentifierType {
    val id = "label-derived"
  }
  val typeMap: Map[String, IdentifierType] =
    Seq(LCSubjects, LCNames, MeSH, LabelDerived).map(i => i.id -> i).toMap
}
