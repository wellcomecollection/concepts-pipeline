package weco.concepts.recorder

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.model.{
  AuthoritativeConcept,
  Identifier,
  IdentifierType,
  CatalogueConcept
}

class MergeConceptsTest extends AnyFunSpec with Matchers {
  it("merges an AuthoritativeConcept and a CatalogueConcept") {
    val authoritativeConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "sh95000541",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "World Wide Web",
      alternativeLabels = Seq(
        "W3 (World Wide Web)",
        "WWW (World Wide Web)",
        "Web (World Wide Web)",
        "World Wide Web (Information retrieval system)"
      )
    )
    val usedConcept = CatalogueConcept(
      identifier = Identifier(
        value = "sh95000541",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "The Internet",
      canonicalId = "123abcde",
      ontologyType = "Concept"
    )
    val result = MergeConcepts(Some(authoritativeConcept), Some(usedConcept))

    result.identifiers shouldBe Seq(authoritativeConcept.identifier)
    result.label shouldBe authoritativeConcept.label
    result.alternativeLabels shouldBe authoritativeConcept.alternativeLabels
    result.canonicalId shouldBe usedConcept.canonicalId
    result.ontologyType shouldBe "Concept"
  }

  it(
    "uses the used concept's ontologyType when merging an AuthoritativeConcept and a CatalogueConcept"
  ) {
    val authoritativeConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "n78095637",
        identifierType = IdentifierType.LCNames
      ),
      label = "Darwin, Charles, 1809-1882",
      alternativeLabels = Seq(
        "Chuck D."
      )
    )
    val usedConcept = CatalogueConcept(
      identifier = Identifier(
        value = "n78095637",
        identifierType = IdentifierType.LCNames
      ),
      label = "Charles Darwin",
      canonicalId = "123abcde",
      ontologyType = "Person"
    )
    val result = MergeConcepts(Some(authoritativeConcept), Some(usedConcept))

    result.identifiers shouldBe Seq(authoritativeConcept.identifier)
    result.label shouldBe authoritativeConcept.label
    result.alternativeLabels shouldBe authoritativeConcept.alternativeLabels
    result.canonicalId shouldBe usedConcept.canonicalId
    result.ontologyType shouldBe "Person"
  }

  it("creates a Concept from a CatalogueConcept without an AuthoritativeConcept") {
    val usedConcept = CatalogueConcept(
      identifier = Identifier(
        value = "things",
        identifierType = IdentifierType.LabelDerived
      ),
      label = "Things",
      canonicalId = "123abcde",
      ontologyType = "Concept"
    )
    val result = MergeConcepts(None, Some(usedConcept))

    result.canonicalId shouldBe usedConcept.canonicalId
    result.identifiers shouldBe Seq(usedConcept.identifier)
    result.label shouldBe usedConcept.label
    result.alternativeLabels shouldBe Nil
    result.ontologyType shouldBe "Concept"
  }

  it("extracts the ontology type from a used concept on its own") {
    val usedConcept = CatalogueConcept(
      identifier = Identifier(
        value = "roland le petour",
        identifierType = IdentifierType.LabelDerived
      ),
      label = "Roland le Petour",
      canonicalId = "123abcde",
      ontologyType = "Person"
    )
    val result = MergeConcepts(None, Some(usedConcept))

    result.canonicalId shouldBe usedConcept.canonicalId
    result.identifiers shouldBe Seq(usedConcept.identifier)
    result.label shouldBe usedConcept.label
    result.alternativeLabels shouldBe Nil
    result.ontologyType shouldBe "Person"
  }

  it("errors if the concepts do not have matching identifiers") {
    val authoritativeConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "sh95000541",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "World Wide Web",
      alternativeLabels = Seq(
        "W3 (World Wide Web)",
        "WWW (World Wide Web)",
        "Web (World Wide Web)",
        "World Wide Web (Information retrieval system)"
      )
    )
    val usedConcept = CatalogueConcept(
      identifier = Identifier(
        value = "sh92002816",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "The Internet",
      canonicalId = "123abcde",
      ontologyType = "Concept"
    )

    the[IllegalArgumentException] thrownBy MergeConcepts(
      Some(authoritativeConcept),
      Some(usedConcept)
    ) should have message s"requirement failed: Cannot merge concepts with different identifiers (${authoritativeConcept.identifier} and ${usedConcept.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
  }

  it("errors if the CatalogueConcept is None") {
    val authoritativeConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "sh95000541",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "World Wide Web",
      alternativeLabels = Seq(
        "W3 (World Wide Web)",
        "WWW (World Wide Web)",
        "Web (World Wide Web)",
        "World Wide Web (Information retrieval system)"
      )
    )

    the[IllegalArgumentException] thrownBy MergeConcepts(
      Some(authoritativeConcept),
      None
    ) should have message s"This error should never occur: we've been asked to merge a concept (${authoritativeConcept.identifier}) which isn't used in the catalogue"
  }

  it("errors if given 2 Nones") {
    the[IllegalArgumentException] thrownBy MergeConcepts(
      None,
      None
    ) should have message "This error should never occur: we've been asked to merge a concept which exists in neither the authoritative nor the used concepts indices. Has something gone wrong in the aggregator?"
  }
}
