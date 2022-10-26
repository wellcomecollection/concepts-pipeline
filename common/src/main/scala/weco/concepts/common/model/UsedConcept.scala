package weco.concepts.common.model

import weco.concepts.common.json.Indexable

/** A UsedConcept represents a concept in use in Works in the Catalogue. It
  * links a Wellcome canonicalId to an identifier from an external authority.
  *
  * The label is included in all records, but only really required for
  * identifiers with no authority (label-derived), where it will be used as the
  * preferred label later in the pipeline. In authority-derived concepts, it
  * will be overridden by the authority's preferred label, but it is helpful to
  * record it anyway to help understand the resulting records in the aggregated
  * concepts database.
  */
case class UsedConcept(
  identifier: Identifier,
  label: String,
  canonicalId: String
) {
  override def toString: String =
    s"\n$canonicalId\t${identifier.toString.padTo(70, ' ')}$label"
}

object UsedConcept {
  import weco.concepts.common.json.JsonOps._

  implicit val indexableUsedConcept: Indexable[UsedConcept] =
    new Indexable[UsedConcept] {
      def id(t: UsedConcept): String = t.identifier.value
      def toDoc(t: UsedConcept): ujson.Value = ujson.Obj(
        "authority" -> t.identifier.identifierType.id,
        "identifier" -> t.identifier.value,
        "label" -> t.label,
        "canonicalId" -> t.canonicalId
      )

      def fromDoc(doc: ujson.Value): Option[UsedConcept] = for {
        id <- doc.opt[String]("identifier")
        canonicalId <- doc.opt[String]("canonicalId")
        authority <- doc.opt[String]("authority")
        identifierType <- IdentifierType.typeMap.get(authority)
        label <- doc.opt[String]("label")
      } yield UsedConcept(
        identifier = Identifier(
          value = id,
          identifierType = identifierType
        ),
        label = label,
        canonicalId = canonicalId
      )
    }
}
