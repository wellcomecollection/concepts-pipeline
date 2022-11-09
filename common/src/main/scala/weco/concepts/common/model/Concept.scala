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

  implicit val indexableConcept: Indexable[Concept] = new Indexable[Concept] {
    def id(t: Concept): String = t.canonicalId

    def toDoc(t: Concept): ujson.Value = ujson.Obj(
      "query" -> ujson.Obj(
        "id" -> t.canonicalId,
        "identifiers" -> t.identifiers.map(id =>
          ujson.Obj(
            "value" -> id.value,
            "identifierType" -> id.identifierType.id
          )
        ),
        "label" -> t.label,
        "alternativeLabels" -> t.alternativeLabels,
        "type" -> t.ontologyType
      ),
      "display" -> ujson.Obj(
        "id" -> t.canonicalId,
        "identifiers" -> t.identifiers.map(id =>
          ujson.Obj(
            "identifierType" -> ujson.Obj(
              "id" -> id.identifierType.id,
              "label" -> id.identifierType.label,
              "type" -> "IdentifierType"
            ),
            "value" -> id.value,
            "type" -> "Identifier"
          )
        ),
        "label" -> t.label,
        "alternativeLabels" -> t.alternativeLabels,
        "type" -> t.ontologyType
      )
    )

    def fromDoc(doc: ujson.Value): Option[Concept] =
      throw new NotImplementedError(
        "Concept deserialization is not currently implemented as indexed concepts are designed to be queried/displayed directly by the API"
      )
  }
}
