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
      "id" -> t.canonicalId,
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
        .opt[ujson.Value]("identifiers")
        .flatMap(Indexable[Seq[Identifier]].fromDoc)
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
  }
}
