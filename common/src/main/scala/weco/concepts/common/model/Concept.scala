package weco.concepts.common.model

import weco.concepts.common.json.Indexable

case class Concept(
  canonicalId: String,
  // Making this an array now to express future intent to merge different concepts
  identifiers: Seq[Identifier],
  label: String,
  alternativeLabels: Seq[String],
  ontologyType: String
)

object Concept {
  import weco.concepts.common.json.JsonOps._

  implicit val indexableConcept: Indexable[Concept] = new Indexable[Concept] {
    def id(t: Concept): String = t.canonicalId
    def toDoc(t: Concept): ujson.Value = ujson.Obj(
      "canonicalId" -> t.canonicalId,
      "identifiers" -> t.identifiers.map(id =>
        ujson.Obj(
          "identifier" -> id.value,
          "authority" -> id.identifierType.id
        )
      ),
      "label" -> t.label,
      "alternativeLabels" -> t.alternativeLabels,
      "type" -> t.ontologyType
    )

    def fromDoc(doc: ujson.Value): Option[Concept] = for {
      canonicalId <- doc.opt[String]("canonicalId")
      identifiers <- doc
        .opt[Seq[ujson.Value]]("identifiers")
        .map(_.flatMap(fromIdentifierDoc))
      label <- doc.opt[String]("label")
      alternativeLabels <- doc.opt[Seq[String]]("alternativeLabels")
      ontologyType <- doc.opt[String]("type")
    } yield Concept(
      canonicalId = canonicalId,
      identifiers = identifiers,
      label = label,
      alternativeLabels = alternativeLabels,
      ontologyType = ontologyType
    )

    private def fromIdentifierDoc(doc: ujson.Value): Option[Identifier] = for {
      value <- doc.opt[String]("identifier")
      authority <- doc.opt[String]("authority")
      identifierType <- IdentifierType.typeMap.get(authority)
    } yield Identifier(
      value = value,
      identifierType = identifierType
    )
  }
}
