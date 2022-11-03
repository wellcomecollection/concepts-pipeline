package weco.concepts.common.model

import weco.concepts.common.json.Indexable

/** A CatalogueConcept represents a concept in use in Works in the Catalogue. It
  * links a Wellcome canonicalId to an identifier from an external authority.
  *
  * The label is included in all records, but only really required for
  * identifiers with no authority (label-derived), where it will be used as the
  * preferred label later in the pipeline. In authority-derived concepts, it
  * will be overridden by the authority's preferred label, but it is helpful to
  * record it anyway to help understand the resulting records in the aggregated
  * concepts database.
  */
case class CatalogueConcept(
  identifier: Identifier,
  label: String,
  canonicalId: String,
  ontologyType: String
) {
  override def toString: String =
    s"\n$canonicalId\t${identifier.toString.padTo(70, ' ')}$label"
}

object CatalogueConcept {
  import weco.concepts.common.json.JsonOps._

  implicit val indexableUsedConcept: Indexable[CatalogueConcept] =
    new Indexable[CatalogueConcept] {
      def id(t: CatalogueConcept): String = t.identifier.toString
      def toDoc(t: CatalogueConcept): ujson.Value = ujson.Obj(
        "authority" -> t.identifier.identifierType.id,
        "identifier" -> t.identifier.value,
        "label" -> t.label,
        "canonicalId" -> t.canonicalId,
        "ontologyType" -> t.ontologyType
      )

      def fromDoc(doc: ujson.Value): Option[CatalogueConcept] = for {
        canonicalId <- doc.opt[String]("canonicalId")
        identifier <- Indexable[Identifier].fromDoc(doc)
        label <- doc.opt[String]("label")
        ontologyType <- doc.opt[String]("ontologyType")
      } yield CatalogueConcept(
        identifier = identifier,
        label = label,
        canonicalId = canonicalId,
        ontologyType = ontologyType
      )
    }
}
