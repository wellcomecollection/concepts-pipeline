package weco.concepts.recorder

import grizzled.slf4j.Logging
import weco.concepts.common.model.{
  AuthoritativeConcept,
  CatalogueConcept,
  Concept
}

object MergeConcepts extends Logging {
  def apply(
    authoritative: Option[AuthoritativeConcept],
    catalogue: Option[CatalogueConcept]
  ): Seq[Concept] = (authoritative, catalogue) match {
    case (Some(authoritative), Some(catalogue)) =>
      info(
        s"Merging ${catalogue.canonicalId} and ${authoritative.identifier.toString}"
      )
      Seq(merge(authoritative, catalogue))
    case (None, Some(catalogue)) =>
      info(s"Forwarding ${catalogue.identifier.toString}")
      Seq(fromCatalogueOnly(catalogue))
    case (Some(authoritative), None) =>
      throw new IllegalArgumentException(
        s"This error should never occur: we've been asked to merge a concept (${authoritative.identifier}) which isn't used in the catalogue"
      )
    case (None, None) =>
      throw new IllegalArgumentException(
        "This error should never occur: we've been asked to merge a concept which exists in neither the authoritative nor the catalogue concepts indices. Has something gone wrong in the aggregator?"
      )
  }

  private def merge(
    authoritative: AuthoritativeConcept,
    catalogue: CatalogueConcept
  ): Concept = {
    require(
      authoritative.identifier == catalogue.identifier,
      s"Cannot merge concepts with different identifiers (${authoritative.identifier} and ${catalogue.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
    )
    Concept(
      canonicalId = catalogue.canonicalId,
      identifiers = Seq(authoritative.identifier),
      label = authoritative.label,
      alternativeLabels = authoritative.alternativeLabels,
      ontologyType = catalogue.ontologyType
    )
  }

  private def fromCatalogueOnly(catalogueConcept: CatalogueConcept): Concept =
    Concept(
      canonicalId = catalogueConcept.canonicalId,
      identifiers = Seq(catalogueConcept.identifier),
      label = catalogueConcept.label,
      alternativeLabels = Nil,
      ontologyType = catalogueConcept.ontologyType
    )
}
