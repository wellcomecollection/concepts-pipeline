package weco.concepts.common.model

import grizzled.slf4j.Logging
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
  val label: String
  override def toString: String = id
}

// These are expected to match those in
// https://github.com/wellcomecollection/catalogue-pipeline/blob/main/common/internal_model/src/main/scala/weco/catalogue/internal_model/identifiers/IdentifierType.scala
object IdentifierType extends Logging {
  def fromId(id: String): Option[IdentifierType] = {
    val identifierType = typeMap.get(id)
    if (identifierType.isEmpty) {
      warn(s"Unexpected identifier type ID: $id")
    }
    identifierType
  }

  case object Fihrist extends IdentifierType {
    val id = "fihrist"
    val label = "Fihrist Authority"
  }
  case object LabelDerived extends IdentifierType {
    val id = "label-derived"
    val label = "Identifier derived from the label of the referent"
  }
  case object LCNames extends IdentifierType {
    val id = "lc-names"
    val label = "Library of Congress Name authority records"
  }
  case object LCSubjects extends IdentifierType {
    val id = "lc-subjects"
    val label = "Library of Congress Subject Headings (LCSH)"
  }
  case object MeSH extends IdentifierType {
    val id = "nlm-mesh"
    val label = "Medical Subject Headings (MeSH) identifier"
  }
  case object Viaf extends IdentifierType {
    val id = "viaf"
    val label = "VIAF: The Virtual International Authority File"
  }

  val types: Set[IdentifierType] =
    Set(Fihrist, LabelDerived, LCNames, LCSubjects, MeSH, Viaf)

  private val typeMap: Map[String, IdentifierType] =
    types
      .map(i => i.id -> i)
      .toMap
}
