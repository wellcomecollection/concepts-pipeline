package weco.concepts.ingestor

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.concepts.common.model.{
  AuthoritativeConcept,
  Identifier,
  IdentifierType
}

class IngestStreamTest extends AnyFunSpec with Matchers {
  it("ignores documents which are geographic subdivision records") {
    val invalidConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "sh2014000619-781",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "Gabon--Parc national de Loango",
      alternativeLabels = Nil
    )
    val validConcept = AuthoritativeConcept(
      identifier = Identifier(
        value = "sh2014000619",
        identifierType = IdentifierType.LCSubjects
      ),
      label = "Parc national de Loango (Gabon)",
      alternativeLabels = Nil
    )

    IngestStream.filterConcepts(invalidConcept) shouldBe false
    IngestStream.filterConcepts(validConcept) shouldBe true
  }
}
