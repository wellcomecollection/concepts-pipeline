package weco.concepts.common.model

case class Identifier(
  value: String,
  identifierType: IdentifierType,
  ontologyType: "Identifier" = "Identifier"
) {
  override def toString: String = s"$identifierType:$value"
}

sealed trait IdentifierType {
  val id: String
  override def toString: String = id
}

object IdentifierType {
  case object Fihrist extends IdentifierType {
    val id = "fihrist"
  }
  case object LabelDerived extends IdentifierType {
    val id = "label-derived"
  }
  case object LCNames extends IdentifierType {
    val id = "lc-names"
  }
  case object LCSubjects extends IdentifierType {
    val id = "lc-subjects"
  }
  case object MeSH extends IdentifierType {
    val id = "nlm-mesh"
  }
  case object Viaf extends IdentifierType {
    val id = "viaf"
  }

  val typeMap: Map[String, IdentifierType] =
    Seq(Fihrist, LabelDerived, LCNames, LCSubjects, MeSH, Viaf).map(i => i.id -> i).toMap
}
