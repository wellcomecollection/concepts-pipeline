package weco.concepts.common.model.matchers

import weco.concepts.common.model.{CatalogueConcept, Identifier}
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

trait CatalogueConceptMatchers {
  def identifier(
    expectedValue: Identifier
  ): HavePropertyMatcher[CatalogueConcept, Identifier] =
    (concept: CatalogueConcept) =>
      HavePropertyMatchResult(
        concept.identifier == expectedValue,
        "identifier",
        expectedValue,
        concept.identifier
      )

  def label(
    expectedValue: String
  ): HavePropertyMatcher[CatalogueConcept, String] =
    (concept: CatalogueConcept) =>
      HavePropertyMatchResult(
        concept.label == expectedValue,
        "label",
        expectedValue,
        concept.label
      )

  def canonicalId(
    expectedValue: String
  ): HavePropertyMatcher[CatalogueConcept, String] =
    (concept: CatalogueConcept) =>
      HavePropertyMatchResult(
        concept.canonicalId == expectedValue,
        "canonicalId",
        expectedValue,
        concept.canonicalId
      )

  def ontologyType(
    expectedValue: String
  ): HavePropertyMatcher[CatalogueConcept, String] =
    (concept: CatalogueConcept) =>
      HavePropertyMatchResult(
        concept.ontologyType == expectedValue,
        "ontologyType",
        expectedValue,
        concept.ontologyType
      )

}
