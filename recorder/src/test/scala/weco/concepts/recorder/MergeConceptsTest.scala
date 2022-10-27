package weco.concepts.recorder

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.model.{
  AuthoritativeConcept,
  Identifier,
  IdentifierType,
  UsedConcept
}

class MergeConceptsTest extends AnyFunSpec with Matchers {
  it("merges an AuthoritativeConcept and a UsedConcept") {
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
    val usedConcept = UsedConcept(
      identifier = Identifier(
        value = "sh95000541",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "The Internet",
      canonicalId = "123abcde"
    )
    val result = MergeConcepts(Some(authoritativeConcept), Some(usedConcept))

    result.identifiers shouldBe Seq(authoritativeConcept.identifier)
    result.label shouldBe authoritativeConcept.label
    result.alternativeLabels shouldBe authoritativeConcept.alternativeLabels
    result.canonicalId shouldBe usedConcept.canonicalId
    result.ontologyType shouldBe "Concept"
  }

  it("creates a Concept from a UsedConcept without an AuthoritativeConcept") {
    val usedConcept = UsedConcept(
      identifier = Identifier(
        value = "things",
        identifierType = IdentifierType.LabelDerived
      ),
      label = "Things",
      canonicalId = "123abcde"
    )
    val result = MergeConcepts(None, Some(usedConcept))

    result.canonicalId shouldBe usedConcept.canonicalId
    result.identifiers shouldBe Seq(usedConcept.identifier)
    result.label shouldBe usedConcept.label
    result.alternativeLabels shouldBe Nil
    result.ontologyType shouldBe "Concept"
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
    val usedConcept = UsedConcept(
      identifier = Identifier(
        value = "sh92002816",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "The Internet",
      canonicalId = "123abcde"
    )

    the[IllegalArgumentException] thrownBy MergeConcepts(
      Some(authoritativeConcept),
      Some(usedConcept)
    ) should have message s"requirement failed: Cannot merge concepts with different identifiers (${authoritativeConcept.identifier} and ${usedConcept.identifier}): if you are seeing this error then assumptions about ordering in the recorder have been broken."
  }

  it("errors if the UsedConcept is None") {
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
    ) should have message "This error should never occur: we've been asked to merge 2 concepts which don't exist"
  }
}
