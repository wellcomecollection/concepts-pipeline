package weco.concepts.recorder

import weco.concepts.common.model.{AuthoritativeConcept, Concept, UsedConcept}

object MergeConcepts {
  def apply(
    authoritative: Option[AuthoritativeConcept],
    used: Option[UsedConcept]
  ): Concept = (authoritative, used) match {
    case (Some(authoritative), Some(used)) => merge(authoritative, used)
    case (None, Some(used))                => fromUsedOnly(used)
    case (Some(authoritative), None) =>
      throw new IllegalArgumentException(
        s"This error should never occur: we've been asked to merge a concept (${authoritative.identifier}) which isn't used in the catalogue"
      )
    case (None, None) =>
      throw new IllegalArgumentException(
        "This error should never occur: we've been asked to merge 2 concepts which don't exist"
      )
  }

  private def merge(
    authoritative: AuthoritativeConcept,
    used: UsedConcept
  ): Concept = {
    require(
      authoritative.identifier == used.identifier,
      s"Cannot merge concepts with different identifiers (${authoritative.identifier} and ${used.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
    )
    Concept(
      canonicalId = used.canonicalId,
      identifiers = Seq(authoritative.identifier),
      label = authoritative.label,
      alternativeLabels = authoritative.alternativeLabels,
      // TODO: deal with ontology types properly here
      ontologyType = "Concept"
    )
  }

  private def fromUsedOnly(usedConcept: UsedConcept): Concept = Concept(
    canonicalId = usedConcept.canonicalId,
    identifiers = Seq(usedConcept.identifier),
    label = usedConcept.label,
    alternativeLabels = Nil,
    // TODO: deal with ontology types properly, likely by getting them from the catalogue
    // and including them in UsedConcept
    ontologyType = "Concept"
  )
}
