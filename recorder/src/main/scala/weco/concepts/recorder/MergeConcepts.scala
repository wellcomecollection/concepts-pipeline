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
      merge(authoritative, catalogue)
    case (None, Some(catalogue)) =>
      info(s"Forwarding ${catalogue.identifier.toString}")
      fromCatalogueOnly(catalogue)
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
  ): Seq[Concept] = {
    require(
      authoritative.identifier == catalogue.identifier,
      s"Cannot merge concepts with different identifiers (${authoritative.identifier} and ${catalogue.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
    )
    catalogue.canonicalId.map(conceptId =>
      Concept(
        canonicalId = conceptId,
        identifiers = Seq(authoritative.identifier),
        label = authoritative.label,
        alternativeLabels = authoritative.alternativeLabels,
        ontologyType = mostSpecificType(catalogue.ontologyType)
      )
    )
  }

  private def fromCatalogueOnly(
    catalogueConcept: CatalogueConcept
  ): Seq[Concept] =
    catalogueConcept.canonicalId.map(conceptId =>
      Concept(
        canonicalId = conceptId,
        identifiers = Seq(catalogueConcept.identifier),
        label = catalogueConcept.label,
        alternativeLabels = Nil,
        ontologyType = mostSpecificType(catalogueConcept.ontologyType)
      )
    )

  private def mostSpecificType(types: Seq[String]): String = {
    // Filter out the vague types.
    val specificTypes = types.filterNot(Seq("Agent", "Concept").contains(_))
    specificTypes match {
      // if there are no more specific types, Agent is more specific than Concept.
      case Nil if types.contains("Agent") => "Agent"
      case Nil                            => "Concept"
      // Ideally, and in most cases, the specific list will have only one entry.
      case List(bestType) => bestType
      // If not, log that there are more than one, and return the head.
      case listOfTypes =>
        warn(
          s"Multiple specific types encountered for the same id: $specificTypes, choosing $listOfTypes.head"
        )
        listOfTypes.head
    }
  }
}
