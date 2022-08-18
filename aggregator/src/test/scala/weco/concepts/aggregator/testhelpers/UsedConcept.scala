package weco.concepts.aggregator.testhelpers

import weco.concepts.aggregator.testhelpers.SourceConcept.{
  aCanonicalId,
  aLabel,
  anAuthority,
  anExternalId
}
import weco.concepts.common.model.{
  Identifier,
  IdentifierType,
  UsedConcept => UsedConceptModel
}

object UsedConcept {
  def apply(
    authority: String = anAuthority,
    identifier: String = anExternalId,
    label: String = aLabel,
    canonicalId: String = aCanonicalId
  ): UsedConceptModel = UsedConceptModel(
    label = label,
    canonicalId = canonicalId,
    identifier = Identifier(
      value = identifier,
      identifierType = IdentifierType.typeMap(authority)
    )
  )

}
