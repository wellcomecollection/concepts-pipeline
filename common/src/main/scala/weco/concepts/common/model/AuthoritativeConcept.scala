package weco.concepts.common.model

import weco.concepts.common.json.Indexable

case class AuthoritativeConcept(
  identifier: Identifier,
  label: String,
  alternativeLabels: Seq[String]
)

object AuthoritativeConcept {
  import weco.concepts.common.json.JsonOps._

  implicit val indexableAuthoritativeConcept: Indexable[AuthoritativeConcept] =
    new Indexable[AuthoritativeConcept] {
      def id(t: AuthoritativeConcept): String = t.identifier.toString
      def toDoc(t: AuthoritativeConcept): ujson.Value = ujson.Obj(
        "authority" -> t.identifier.identifierType.id,
        "identifier" -> t.identifier.value,
        "label" -> t.label,
        "alternativeLabels" -> t.alternativeLabels
      )

      def fromDoc(doc: ujson.Value): Option[AuthoritativeConcept] = for {
        id <- doc.opt[String]("identifier")
        authority <- doc.opt[String]("authority")
        identifierType <- IdentifierType.typeMap.get(authority)
        label <- doc.opt[String]("label")
        alternativeLabels <- doc.opt[Seq[String]]("alternativeLabels")
      } yield AuthoritativeConcept(
        identifier = Identifier(
          value = id,
          identifierType = identifierType
        ),
        label = label,
        alternativeLabels = alternativeLabels
      )
    }
}
