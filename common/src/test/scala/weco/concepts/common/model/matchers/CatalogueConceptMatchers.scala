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
        concept.canonicalId == Seq(expectedValue),
        "canonicalId",
        expectedValue,
        concept.canonicalId.toString()
      )

  def ontologyType(
    expectedValue: String
  ): HavePropertyMatcher[CatalogueConcept, String] =
    (concept: CatalogueConcept) =>
      HavePropertyMatchResult(
        concept.ontologyType == Seq(expectedValue),
        "ontologyType",
        expectedValue,
        concept.ontologyType.toString()
      )

}
