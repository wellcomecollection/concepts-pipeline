package weco.concepts.common.model

import weco.concepts.common.json.Indexable

case class Identifier(
  value: String,
  identifierType: IdentifierType,
  ontologyType: "Identifier" = "Identifier"
) {
  override def toString: String = s"$identifierType:$value"
}

object Identifier {
  import weco.concepts.common.json.JsonOps._

  implicit val indexableIdentifier: Indexable[Identifier] =
    new Indexable[Identifier] {
      def id(t: Identifier): String = t.toString
      def toDoc(t: Identifier): ujson.Value = ujson.Obj(
        "identifier" -> t.value,
        "authority" -> t.identifierType.id
      )

      def fromDoc(doc: ujson.Value): Option[Identifier] = for {
        value <- doc.opt[String]("identifier")
        authority <- doc.opt[String]("authority")
        identifierType <- IdentifierType.fromId(authority)
      } yield Identifier(
        value = value,
        identifierType = identifierType
      )
    }
}

sealed trait IdentifierType {
  val id: String
  override def toString: String = id
}

object IdentifierType {
  def fromId(id: String): Option[IdentifierType] = typeMap.get(id)

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

  val types: Set[IdentifierType] =
    Set(Fihrist, LabelDerived, LCNames, LCSubjects, MeSH, Viaf)

  private val typeMap: Map[String, IdentifierType] =
    types
      .map(i => i.id -> i)
      .toMap
}
